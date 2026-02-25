package io.github.jtpadilla.example.googleapi.apifuture;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.SettableApiFuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ApiFutureExample {

    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public static void main(String[] args) throws Exception {
        System.out.println("--- Ejecutando ejemplo de transform ---");
        transformExample();

        System.out.println("\n--- Ejecutando ejemplo de allAsList ---");
        allAsListExample();

        System.out.println("\n--- Ejecutando ejemplo de successfulAsList ---");
        successfulAsListExample();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Demuestra el uso de ApiFutures.transform.
     */
    private static void transformExample() throws ExecutionException, InterruptedException {
        // Este future se resolverá con un string que parece un número.
        ApiFuture<String> future1 = queryDatabaseAsync("123", executor);

        // Usamos transform para convertir el resultado de string a integer.
        // El error original estaba aquí: queryDatabaseAsync era demasiado genérico.
        // Al hacer que devuelva ApiFuture<String>, el compilador ahora puede verificar
        // que la salida de future1 puede pasarse a Integer::parseInt.
        ApiFuture<Integer> future2 = ApiFutures.transform(future1, Integer::parseInt, executor);

        System.out.println("Resultado del future original: " + future1.get());
        System.out.println("Resultado del future transformado: " + future2.get());
    }

    /**
     * Demuestra el uso de ApiFutures.allAsList.
     */
    private static void allAsListExample() throws ExecutionException, InterruptedException {
        ApiFuture<String> future1 = queryDatabaseAsync("data1", executor);
        ApiFuture<String> future2 = queryDatabaseAsync("data2", executor);

        // El error original estaba aquí: se pasaban los futures como argumentos separados.
        // La solución es envolverlos en una lista.
        ApiFuture<List<String>> combinedFuture = ApiFutures.allAsList(Arrays.asList(future1, future2));

        List<String> results = combinedFuture.get();
        System.out.println("Resultados combinados de allAsList: " + results);
    }

    /**
     * Demuestra el uso de ApiFutures.successfulAsList.
     */
    private static void successfulAsListExample() throws ExecutionException, InterruptedException {
        ApiFuture<String> future1 = queryDatabaseAsync("success1", executor);
        ApiFuture<String> future2 = queryDatabaseAsync("will-fail", executor); // Este fallará.
        ApiFuture<String> future3 = queryDatabaseAsync("success2", executor);

        // El error original estaba aquí: se pasaban los futures como argumentos separados.
        // La solución es envolverlos en una lista.
        ApiFuture<List<String>> successfulAsListFuture = ApiFutures.successfulAsList(Arrays.asList(future1, future2, future3));

        // .get() solo devolverá los resultados de los futures exitosos.
        List<String> successfulResults = successfulAsListFuture.get().stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        System.out.println("Resultados exitosos de successfulAsList: " + successfulResults);
    }

    /**
     * Simula una consulta asíncrona a una base de datos.
     * <p>
     * El error original provenía de que este método era demasiado genérico (p. ej., devolvía ApiFuture<T>).
     * Dado que siempre produce un String, su firma se ha corregido para que devuelva ApiFuture<String>.
     * Esto permite al compilador de Java realizar la comprobación de tipos correctamente, especialmente para las operaciones de transformación.
     *
     * @param query    La consulta simulada.
     * @param executor El servicio de ejecutor en el que se ejecutará.
     * @return Un ApiFuture que se completará con el resultado del string.
     */
    private static ApiFuture<String> queryDatabaseAsync(String query, ExecutorService executor) {
        SettableApiFuture<String> future = SettableApiFuture.create();
        executor.submit(() -> {
            try {
                // Simula la latencia de red/base de datos
                TimeUnit.MILLISECONDS.sleep(500);
                if ("will-fail".equals(query)) {
                    future.setException(new RuntimeException("La consulta a la base de datos falló para: " + query));
                } else {
                    future.set("Resultado para: " + query);
                }
            } catch (Exception e) {
                future.setException(e);
            }
        });
        return future;
    }
}
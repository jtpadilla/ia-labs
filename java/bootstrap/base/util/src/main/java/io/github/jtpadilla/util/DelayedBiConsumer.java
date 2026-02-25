package io.github.jtpadilla.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class DelayedBiConsumer<T, U> {

    private final AtomicReference<BiConsumer<T, U>> consumerRef = new AtomicReference<>();

    /**
     * Configura el BiConsumer.  Puede ser llamado múltiples veces,
     * pero solo el último BiConsumer configurado será el que se use.
     *
     * @param consumer El BiConsumer a ejecutar.
     */
    public void setConsumer(BiConsumer<T, U> consumer) {
        consumerRef.set(consumer);
    }

    /**
     * Ejecuta el BiConsumer si está configurado. Si no está configurado,
     * no hace nada (no lanza excepción).  Es seguro para usar en
     * entornos multi-hilo.
     *
     * @param t El primer argumento para el BiConsumer.
     * @param u El segundo argumento para el BiConsumer.
     */
    public void accept(T t, U u) {
        BiConsumer<T, U> consumer = consumerRef.get();
        if (consumer != null) {
            consumer.accept(t, u);
        }
    }

    /**
     * Verifica si el BiConsumer ha sido configurado.
     *
     * @return  {@code true} si el BiConsumer se ha configurado (no es nulo),
     *          {@code false} en caso contrario.
     */
    public boolean isConfigured() {
        return consumerRef.get() != null;
    }

    /**
     *  Intenta ejecutar el BiConsumer.
     *  Si el BiConsumer no está configurado, lanza una excepción.
     *  Esta es una alternativa a {@link #accept(Object, Object)}
     *  para casos donde se *requiere* que el BiConsumer esté presente.
     * @param t El primer argumento
     * @param u El segundo argumento
     * @throws IllegalStateException Si el BiConsumer no se ha configurado todavía.
     */
    public void acceptOrThrow(T t, U u) {
        BiConsumer<T, U> consumer = consumerRef.get();
        if (consumer == null) {
            throw new IllegalStateException("El BiConsumer no ha sido configurado.");
        }
        consumer.accept(t, u);
    }

    /**
     * Limpia (establece a null) el BiConsumer almacenado.  Útil si, por ejemplo,
     * se quiere liberar la referencia al BiConsumer después de que se haya
     * utilizado un número determinado de veces o bajo ciertas condiciones.
     */
    public void clear() {
        consumerRef.set(null);
    }


    /**
     * Ejecuta el BiConsumer si está configurado. Si no está configurado,
     * ejecuta el Runnable proporcionado.
     *
     * @param t       El primer argumento para el BiConsumer.
     * @param u       El segundo argumento para el BiConsumer.
     * @param onNotConfigured El Runnable a ejecutar si el BiConsumer no está configurado.
     */
    public void acceptOrElse(T t, U u, Runnable onNotConfigured) {
        BiConsumer<T, U> consumer = consumerRef.get();
        if (consumer != null) {
            consumer.accept(t, u);
        } else {
            onNotConfigured.run();
        }
    }

}
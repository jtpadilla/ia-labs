package io.github.jtpadilla.example.gemma3;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.time.Duration;

public class Gemma3Client {

    private static final String SERVICE_URL = "https://sc-gemma3-176809830110.europe-west1.run.app/v1/chat/completions";

    private static final String MODEL_NAME = "/app/gemma-3-270m";


    public static String llamarModeloRiego(String promptUsuario) throws IOException, InterruptedException {

        System.out.println("Enviando prompt al modelo: " + promptUsuario);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();

        String jsonBody = """
                {
                    "model": "%s",
                    "messages": [
                        {"role": "system", "content": "Eres un experto en riego inteligente."},
                        {"role": "user", "content": "%s"}
                    ],
                    "temperature": 0.3,
                    "max_tokens": 150
                }
                """.formatted(MODEL_NAME, promptUsuario);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVICE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofMinutes(5))
                .build();

        System.out.println("Enviando petición a: " + SERVICE_URL);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Respuesta recibida (código): " + response.statusCode());

        return response.body();
    }

    public static void main(String[] args) {

        System.out.println("--- Realizando llamada al modelo desplegado... ---");
        System.out.println("--- (La primera llamada puede tardar por el arranque en frío) ---");

        String promptDePrueba = "Datos: Humedad=35%, Temp=28C, Previsión=Sol. ¿Activar riego? Responde SÍ o NO y explica por qué brevemente.";
        try {
            String respuestaJson = llamarModeloRiego(promptDePrueba);

            System.out.println("\n--- Respuesta completa (JSON): ---");
            System.out.println(respuestaJson);


        } catch (IOException | InterruptedException e) {
            System.err.println("Error al llamar al modelo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
package io.github.jtpadilla.example.interactions.demo.researchfrontend;

import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.jtpadilla.example.interactions.demo.researchfrontend.impl.DisplayError;
import io.github.jtpadilla.example.interactions.demo.researchfrontend.impl.UserInterface;
import io.javelit.core.Server;

public class Launcher {

    static void main(String[] args) {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null) {
            Server.builder(DisplayError::javelit, 8080).build().start();

        } else {
            final GeminiInteractionsClient client = GeminiInteractionsClient.builder()
                    .apiKey(apiKey)
                    .build();
            Server.builder(() -> new UserInterface(client), 8080)
                    .build()
                    .start();
        }

    }

}

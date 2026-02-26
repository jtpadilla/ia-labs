package io.github.jtpadilla.example.interactions.demo.researchfrontend;

import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.jtpadilla.example.interactions.demo.researchfrontend.impl.ResearchServer;
import io.javelit.core.Jt;
import io.javelit.core.Server;

public class Launcher {

    static void main(String[] args) {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null) {
            Server.builder(() -> Jt.error("GEMINI_API_KEY environment variable not set").use(), 8080).build().start();
            return;
        }

        final GeminiInteractionsClient client = GeminiInteractionsClient.builder()
                .apiKey(apiKey)
                .build();

        Server.builder(() -> new ResearchServer(client), 8080)
                .build()
                .start();

    }

}

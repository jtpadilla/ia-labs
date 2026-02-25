package io.github.jtpadilla.example.interactions.simple;

import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.Events;
import io.github.glaforge.gemini.interactions.model.InteractionParams;

class StreamingResponse {

    static void main(String[] args) {

        final GeminiInteractionsClient client = GeminiInteractionsClient.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();

        final InteractionParams.ModelInteractionParams request = InteractionParams.ModelInteractionParams.builder()
                .model("gemini-3-flash-preview")
                .input("¿Porque el cielo es azul?")
                .stream(true)
                .build();

        client.stream(request).forEach(event -> {
            if (event instanceof Events.ContentDelta delta) {
                if (delta.delta() instanceof Events.TextDelta textPart) {
                    System.out.print(textPart.text());
                }
            }
        });

    }

}

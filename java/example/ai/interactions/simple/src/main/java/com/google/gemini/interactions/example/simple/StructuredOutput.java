package com.google.gemini.interactions.example.simple;

import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.Events;
import io.github.glaforge.gemini.interactions.model.InteractionParams.ModelInteractionParams;
import java.util.Map;

public class StructuredOutput {

    static void main(String[] args) {

        final GeminiInteractionsClient client = GeminiInteractionsClient.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();

        ModelInteractionParams params = ModelInteractionParams.builder()
                .model("gemini-2.5-flash")
                .input("List 5 popular cookie recipes")
                .stream(true)
                .responseMimeType("application/json")
                .responseFormat(Map.of(
                        "type", "array",
                        "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "recipe_name", Map.of("type", "string")
                                )
                        )
                ))
                .build();

        /*
        Interaction response = client.create(params);
        Util.dumpThoughts(Util.getThoughts(response));
        Util.dumpText(Util.getText(response));
        */

        client.stream(params).forEach(event -> {
            if (event instanceof Events.ContentDelta delta) {
                if (delta.delta() instanceof Events.TextDelta textPart) {
                    System.out.print(textPart.text());
                }
            }
        });

    }

}

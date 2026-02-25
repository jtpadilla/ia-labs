package io.github.jtpadilla.example.interactions.simple;

import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.Events;
import io.github.glaforge.gemini.interactions.model.InteractionParams.ModelInteractionParams;

import static io.github.glaforge.gemini.schema.GSchema.*;

public class StructuredOutputSchemaBuilder {

    static void main(String[] args) {

        final GeminiInteractionsClient client = GeminiInteractionsClient.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();

        ModelInteractionParams params = ModelInteractionParams.builder()
                .model("gemini-2.5-flash")
                .input("List 5 popular cookie recipes")
                .stream(true)
                .responseMimeType("application/json")
                .responseFormat(
                        arr().items(
                                obj()
                                        .prop("recipe_name", str())
                                        .prop("ingredients", arr().items(str()))
                        )
                )
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

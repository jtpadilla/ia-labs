package io.github.jtpadilla.example.interactions.simple;

import io.github.jtpadilla.example.interactions.util.Util;
import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.Interaction;
import io.github.glaforge.gemini.interactions.model.InteractionParams;

import static io.github.glaforge.gemini.interactions.model.Interaction.Role.MODEL;
import static io.github.glaforge.gemini.interactions.model.Interaction.Role.USER;

class MultiturnConversation {

    static void main(String[] args) {

        final GeminiInteractionsClient client = GeminiInteractionsClient.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();

        InteractionParams.ModelInteractionParams request = InteractionParams.ModelInteractionParams.builder()
                .model("gemini-3-flash-preview")
                .input(
                        new Interaction.Turn(USER, "Hello!"),
                        new Interaction.Turn(MODEL, "Hi! How can I help?"),
                        new Interaction.Turn(USER, "Tell me a joke")
                )
                .build();
        final Interaction response = client.create(request);

        Util.dumpText(Util.getText(response));

    }
    
}

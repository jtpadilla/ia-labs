package io.github.jtpadilla.example.interactions.simple;

import io.github.jtpadilla.example.interactions.util.Util;
import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.Interaction;
import io.github.glaforge.gemini.interactions.model.InteractionParams;

class SimpleTextInteraction {

    static void main(String[] args) {

        final GeminiInteractionsClient client = GeminiInteractionsClient.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();

        final InteractionParams.ModelInteractionParams request = InteractionParams.ModelInteractionParams.builder()
                .model("gemini-3.1-pro-preview")
                //.model("gemini-3-flash-preview")
                .input("¿Porque el cielo es azul?")
                .build();

        Interaction response = client.create(request);
        Util.dumpThoughts(Util.getThoughts(response));
        Util.dumpText(Util.getText(response));

    }
    
}

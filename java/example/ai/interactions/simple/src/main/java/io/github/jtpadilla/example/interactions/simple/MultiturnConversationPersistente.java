package io.github.jtpadilla.example.interactions.simple;

import io.github.jtpadilla.example.interactions.util.Util;
import io.github.glaforge.gemini.interactions.GeminiInteractionsClient;
import io.github.glaforge.gemini.interactions.model.Interaction;
import io.github.glaforge.gemini.interactions.model.InteractionParams;

class MultiturnConversationPersistente {

    static void main(String[] args) {

        final GeminiInteractionsClient client = GeminiInteractionsClient.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();

        // 1. First turn (must set store=true)
        InteractionParams.ModelInteractionParams turn1 = InteractionParams.ModelInteractionParams.builder()
                .model("gemini-3-flash-preview")
                .input("Hola!")
                .store(true)
                .build();

        Interaction response1 = client.create(turn1);
        String id = response1.id();
        System.out.println("Id -> " + response1.id());
        Util.dumpText(Util.getText(response1));

        // 2. Second turn (referencing previous ID)
        InteractionParams.ModelInteractionParams turn2 = InteractionParams.ModelInteractionParams.builder()
                .model("gemini-3-flash-preview")
                .input("Cuentame un chiste")
                .previousInteractionId(id)
                .store(true) // Optional if you want to extend further
                .build();

        Interaction response2 = client.create(turn2);
        Util.dumpText(Util.getText(response2));

    }
    
}

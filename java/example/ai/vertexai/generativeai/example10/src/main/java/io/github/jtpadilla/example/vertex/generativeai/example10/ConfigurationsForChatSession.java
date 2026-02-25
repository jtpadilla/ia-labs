package io.github.jtpadilla.example.vertex.generativeai.example10;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.ChatSession;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import io.github.jtpadilla.example.vertex.common.VertextaiUtil;

import java.io.IOException;

// Construye la instancia de GenerativeModel mediante un builder el cual
// permite proporcionarle la configuracion al modelo.
//
// Despues aprovecha el interface fluido del CharSession para actualizar
// sobre la marcha la configuracion del modelo con el que esta trabajando
// el chat.
public class ConfigurationsForChatSession {

    static final private String MODEL_NAME = "gemini-2.0-flash";

    public static void main(String[] args) throws IOException {

        try (VertexAI vertexAi = VertextaiUtil.vertexBuilder().build()) {

            // Instantiate a model with GenerationConfig
            final GenerationConfig generationConfig = GenerationConfig.newBuilder()
                .setMaxOutputTokens(50)
                .build();

            // Se instania el modelo
            final GenerativeModel model = new GenerativeModel.Builder()
                .setModelName(MODEL_NAME)
                .setVertexAi(vertexAi)
                .setGenerationConfig(generationConfig)
                .build();

            // Start a chat session
            final ChatSession chatSession = model.startChat();

            // Send a message. The model level GenerationConfig will be applied here
            final GenerateContentResponse firstResponse = chatSession
                    .sendMessage("Please explain LLM?");
            System.out.println("=======================================================================================");
            System.out.println(ResponseHandler.getText(firstResponse));

            // Send another message, using Fluent API to update the GenerationConfig
            final GenerateContentResponse secondResponse = chatSession
                    .withGenerationConfig(GenerationConfig.getDefaultInstance())
                    .sendMessage("Tell me more about what you can do.");
            System.out.println("=======================================================================================");
            System.out.println(ResponseHandler.getText(secondResponse));

        }
    }

}

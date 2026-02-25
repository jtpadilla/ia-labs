package io.github.jtpadilla.example.vertex.generativeai.example07;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import io.github.jtpadilla.example.vertex.common.VertextaiUtil;

import java.io.IOException;

// Construye la instancia de GenerativeModel mediante un builder el cual
// permite proporcioarle la configuracion que previamente ha estipulado
// la maxima salida en 50 tokens.
public class ModelLevelConfigurations {

    static final private String MODEL_NAME = "gemini-2.0-flash";

    public static void main(String[] args) throws IOException {

        try (VertexAI vertexAi = VertextaiUtil.vertexBuilder().build()) {

            // Build a GenerationConfig instance.
            final GenerationConfig generationConfig = GenerationConfig.newBuilder()
                .setMaxOutputTokens(50)
                .build();

            // Use the builder to instantialize the model with the configuration.
            final GenerativeModel model = new GenerativeModel.Builder()
                .setModelName(MODEL_NAME)
                .setVertexAi(vertexAi)
                .setGenerationConfig(generationConfig)
                .build();

            // Generate the response.
            final GenerateContentResponse response = model.generateContent("Please explain LLM?");
            System.out.println(ResponseHandler.getText(response));

        }
    }

}

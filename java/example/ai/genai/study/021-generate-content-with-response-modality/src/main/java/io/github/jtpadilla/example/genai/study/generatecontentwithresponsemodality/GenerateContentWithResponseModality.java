package io.github.jtpadilla.example.genai.study.generatecontentwithresponsemodality;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.gcloud.genai.IGenAIService;

public class GenerateContentWithResponseModality {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            final GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("TEXT", "IMAGE")
                    .build();

            final GenerateContentResponse response = client.models.generateContent(
                    "imagen-3.0-generate-002",
                    "Generate a cat image and describe it.",
                    config
            );

            System.out.println("Response: " + response.toJson());

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}

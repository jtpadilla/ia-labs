package io.github.jtpadilla.example.genai.study.requestlevelhttpoptions;

import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.gcloud.genai.IGenAIService;

public class RequestLevelHttpOptions {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            // Set a customized header per request config.
            GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .httpOptions(HttpOptions.builder().headers(ImmutableMap.of("my-header", "my-value")))
                            .build();

            GenerateContentResponse response =
                    client.models.generateContent(
                            genAIService.getLlmModel(),
                            "Tell me the history of LLM",
                            config
                    );

            System.out.println("Response: " + response.text());

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}

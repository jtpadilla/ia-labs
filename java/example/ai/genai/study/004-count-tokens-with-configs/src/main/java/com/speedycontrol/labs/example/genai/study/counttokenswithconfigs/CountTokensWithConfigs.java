package com.speedycontrol.labs.example.genai.study.counttokenswithconfigs;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.CountTokensConfig;
import com.google.genai.types.CountTokensResponse;
import com.google.genai.types.Part;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

public final class CountTokensWithConfigs {

    public static void main(String[] args) {
        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();
        try (Client client = genAIService.createClient()) {
            // Se preparan instrucciones del sistema
            final Content systemInstruction = Content.fromParts(Part.fromText("You are a history teacher."));
            // Las instrucciones de sistema a la configuracion
            final CountTokensConfig config =
                    CountTokensConfig.builder()
                            .systemInstruction(systemInstruction)
                            .build();
            final CountTokensResponse response = client.models.countTokens(
                    genAIService.getLlmModel(),
                    "Cuentame una historia de LLM",
                    config
            );
            displayResponse(response);
        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }
    }

    private static void displayResponse(CountTokensResponse response) {
        System.out.format(
                "totalTokens=%s, cachedContentTokenCount=%s",
                response.totalTokens().isPresent() ? Integer.toString(response.totalTokens().get()) : "Empty",
                response.cachedContentTokenCount().isPresent() ? Integer.toString(response.cachedContentTokenCount().get()) : "Empty"
        );
    }

}

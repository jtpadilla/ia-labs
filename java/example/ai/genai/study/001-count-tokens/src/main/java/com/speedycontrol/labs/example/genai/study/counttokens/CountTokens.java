package com.speedycontrol.labs.example.genai.study.counttokens;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.CountTokensConfig;
import com.google.genai.types.CountTokensResponse;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

public final class CountTokens {

    public static void main(String[] args) {
        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();
        try (Client client = genAIService.createClient()) {
            final CountTokensResponse response = client.models.countTokens(
                    genAIService.getLlmModel(),
                    "What is your name?",
                    CountTokensConfig.builder().build()
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

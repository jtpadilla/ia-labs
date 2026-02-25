package com.speedycontrol.labs.example.genai.study.generatecontent;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

public final class GenerateContent {

    public static void main(String[] args) {
        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();
        try (Client client = genAIService.createClient()) {
          final GenerateContentResponse response = client.models.generateContent(
                  genAIService.getLlmModel(),
                  "What is your name?",
                  GenerateContentConfig.builder().build()
          );
          System.out.println("Unary response: " + response.text());
        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }
    }

}

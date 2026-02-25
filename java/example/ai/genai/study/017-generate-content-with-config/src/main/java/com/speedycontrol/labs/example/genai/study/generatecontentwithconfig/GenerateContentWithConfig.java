package com.speedycontrol.labs.example.genai.study.generatecontentwithconfig;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.*;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

public class GenerateContentWithConfig {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            // Sets the safety settings in the config.
            final ImmutableList<SafetySetting> safetySettings =
                    ImmutableList.of(
                            SafetySetting.builder()
                                    .category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH)
                                    .threshold(HarmBlockThreshold.Known.BLOCK_ONLY_HIGH)
                                    .build(),
                            SafetySetting.builder()
                                    .category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT)
                                    .threshold(HarmBlockThreshold.Known.BLOCK_LOW_AND_ABOVE)
                                    .build());

            // Sets the system instruction in the config.
            final Content systemInstruction = Content.fromParts(
                    Part.fromText("You are a history teacher speaking always in spanish.")
            );

            // Sets the Google Search tool in the config.
            final Tool googleSearchTool = Tool.builder().googleSearch(GoogleSearch.builder()).build();

            final GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            // Sets the thinking budget to 0 to disable thinking mode
                            .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0))
                            .candidateCount(1)
                            .maxOutputTokens(1024)
                            .safetySettings(safetySettings)
                            .systemInstruction(systemInstruction)
                            .tools(googleSearchTool)
                            .build();

            final GenerateContentResponse response =
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

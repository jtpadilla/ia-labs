package com.speedycontrol.labs.example.genai.study.embedcontentwithconfig;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

public class EmbedContentWithConfig {


    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            final EmbedContentConfig config = EmbedContentConfig.builder().outputDimensionality(10).build();

            final EmbedContentResponse response =
                    client.models.embedContent(
                            genAIService.getLlmModel(),
                            ImmutableList.of("why is the sky blue?", "What is your age?"),
                    config
            );

            System.out.println("Embedding response: " + response);
        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}

package com.speedycontrol.labs.example.genai.study.generatecontentwithresponseschema;

import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

public class GenerateContentWithResponseSchema {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();
        try (Client client = genAIService.createClient()) {

            final Schema schema =
                    Schema.builder()
                            .type(Type.Known.ARRAY)
                            .items(
                                    Schema.builder()
                                            .type(Type.Known.OBJECT)
                                            .properties(
                                                    ImmutableMap.of(
                                                            "recipe_name",
                                                            Schema.builder().type(Type.Known.STRING).build(),
                                                            "ingredients",
                                                            Schema.builder()
                                                                    .type(Type.Known.ARRAY)
                                                                    .items(Schema.builder().type(Type.Known.STRING))
                                                                    .build()))
                                            .required("recipe_name", "ingredients"))
                            .build();

            final GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .responseMimeType("application/json")
                            .candidateCount(1)
                            .responseSchema(schema)
                            .build();

            final GenerateContentResponse response =
                    client.models.generateContent(genAIService.getLlmModel(), "List a few popular cookie recipes.", config);

            System.out.println("Response: " + response.text());

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}

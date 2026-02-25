package io.github.jtpadilla.example.genai.study.textgenerationwithsysteminstructions;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.gcloud.genai.IGenAIService;

public class GenerateContentWithSystemInstructions {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            final GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .systemInstruction(
                                    Content.fromParts(
                                            Part.fromText("You're a language translator."),
                                            Part.fromText("Your mission is to translate text in English to French.")))
                            .build();

            final GenerateContentResponse response = client.models.generateContent(
                    genAIService.getLlmModel(),
                    "Why is the sky blue?",
                    config
            );

            System.out.print(response.text());

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}

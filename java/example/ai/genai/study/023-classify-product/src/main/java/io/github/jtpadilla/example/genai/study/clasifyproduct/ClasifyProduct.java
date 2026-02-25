package io.github.jtpadilla.example.genai.study.clasifyproduct;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.gcloud.genai.IGenAIService;

import java.util.List;

public class ClasifyProduct {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            final Schema responseSchema =
                    Schema.builder()
                            .type(Type.Known.STRING)
                            .enum_(List.of("Percussion", "String", "Woodwind", "Brass", "Keyboard"))
                            .build();

            final GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .responseMimeType("text/x.enum")
                            .responseSchema(responseSchema)
                            .build();

            final GenerateContentResponse response = client.models.generateContent(
                    genAIService.getLlmModel(),
                    "What type of instrument is an oboe?",
                    config
            );

            System.out.print(response.text());

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}

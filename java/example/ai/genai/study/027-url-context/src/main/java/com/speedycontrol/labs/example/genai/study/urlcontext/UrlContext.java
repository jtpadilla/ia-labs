package com.speedycontrol.labs.example.genai.study.urlcontext;

import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Tool;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

public class UrlContext {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            // Queremos analizar una web
            final Tool urlContextTool = Tool.builder()
                    .urlContext(com.google.genai.types.UrlContext.builder().build())
                    .build();

            // Preparamos la peticion de generacion de contenido
            final GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .tools(urlContextTool)
                            .httpOptions(HttpOptions.builder().headers(ImmutableMap.of("my-header", "my-value")))
                            .build();

            final GenerateContentResponse response =
                    client.models.generateContent(
                            genAIService.getLlmModel(),
                            "Hazme un resumen muy completo de la web https://www.morella.net formateando un poco la salida",
                            config
                    );

            System.out.println("Response: " + response.text());

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}

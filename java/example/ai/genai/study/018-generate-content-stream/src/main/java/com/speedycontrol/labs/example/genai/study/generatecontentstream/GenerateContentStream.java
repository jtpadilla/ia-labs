package com.speedycontrol.labs.example.genai.study.generatecontentstream;

import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentResponse;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

public class GenerateContentStream {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            final ResponseStream<GenerateContentResponse> responseStream =
                    client.models.generateContentStream(
                            genAIService.getLlmModel(),
                            "Tell me a story in 300 words.",
                            null
                    );

            System.out.println("Streaming response: ");
            for (GenerateContentResponse res : responseStream) {
                System.out.print(res.text());
            }

            // To save resources and avoid connection leaks, it is recommended to close the response
            // stream after consumption (or using try block to get the response stream).
            responseStream.close();
            
        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}

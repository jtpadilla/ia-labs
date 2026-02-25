package io.github.jtpadilla.example.vertex.generativeai.example01;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import io.github.jtpadilla.example.vertex.common.VertextaiUtil;
import io.github.jtpadilla.protobuf.ProtoJsonUtil;

import java.io.IOException;

// Primer analisis..
//
// Conclusion, es una llamada a service GRPC GenerativeService
// service GenerativeService {
//   rpc GenerateContent(GenerateContentRequest) returns (GenerateContentResponse)
//   ..
// }
public class BasedTextGeneration {

    static final private String MODEL_NAME = "gemini-2.0-flash";

    public static void main(String[] args) throws IOException {

        // Con el VertexAI.Builder generamos una instancia de VertexAI
        // VertexAI es Autocloseable asi que utilizamos esta funcionalidad
        // para que a la salida del contexto se cierre.
        try (VertexAI vertexAi = VertextaiUtil.vertexBuilder().build()) {

            // Esta instancia es en realidad quien interactuara con el modelo
            // y se puede utilizar en entornos multithread.
            // Los objetos generados por el metodo startChat() no son threads-safe
            //
            // Por lo que se ve GenerativeModel es un envoltorio para invocar a GenerativeService del proto https://github.com/googleapis/googleapis/blob/master/google/ai/generativelanguage/v1/generative_service.proto

            final GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAi);

            // Este metodo genera una respuesta basandose en el cotenido que le proporcionamos
            // GenerativeModel tiene los metodos:
            // - generateContent(Content content): GenerateContentResponse
            // - generateContent(List<Content> contents): GenerateContentResponse
            // - generateContent(String text): GenerateContentResponse
            // El generateContent(String text)  parece una utilidad para invocar
            // a generateContent(Content content).
            //
            // Context es un proto: https://github.com/googleapis/googleapis/blob/master/google/ai/generativelanguage/v1/content.proto
            final GenerateContentResponse response = model.generateContent("How are you?");

            // GenerateContextResponse este tambien otro proto -> https://github.com/googleapis/googleapis/blob/master/google/ai/generativelanguage/v1/generative_service.proto
            // Y es enorme...
            // Asi que en este ejemplo vamos a volcarlo completo para ver que cosas tiene informadas...
            System.out.println(ProtoJsonUtil.toJson(response));

            // Es curioso! model GenerateContentResponse.model_version esta el el proto pero
            // no en el codigo java generado...
//            List<Candidate> candidatesList = response.getCandidatesList();
//            GenerateContentResponse.PromptFeedback promptFeedback = response.getPromptFeedback();
//            GenerateContentResponse.UsageMetadata usageMetadata = response.getUsageMetadata();
//            response.getModelVersion();

        }
    }

}
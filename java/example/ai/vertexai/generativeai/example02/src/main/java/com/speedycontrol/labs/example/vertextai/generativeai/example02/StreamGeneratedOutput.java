package com.speedycontrol.labs.example.vertextai.generativeai.example02;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseStream;
import com.speedycontrol.labs.example.vertextai.common.VertextaiUtil;
import io.github.jtpadilla.protobuf.ProtoJsonUtil;

import java.io.IOException;

// Continacion del example01
//
// Lo interesate es que llama al mismo servicio pero el retorno es un stream de resultados....
// Aunque alfinal lo convierte en una llamada unitaria (convierte el stream que puede ser infinito en una lista finita)
// service GenerativeService {
//   rpc GenerateContent(GenerateContentRequest) returns (stream GenerateContentResponse)
//   ..
// }
public class StreamGeneratedOutput {

    static final private String MODEL_NAME = "gemini-2.0-flash";

    public static void main(String[] args) throws IOException {

        try (VertexAI vertexAi = VertextaiUtil.vertexBuilder().build()) {

            final GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAi);

            final ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream("How are you?");

            for (GenerateContentResponse response : responseStream) {
                System.out.println("-------------------------------------");
                System.out.println(ProtoJsonUtil.toJson(response));
            }

        }
    }

}

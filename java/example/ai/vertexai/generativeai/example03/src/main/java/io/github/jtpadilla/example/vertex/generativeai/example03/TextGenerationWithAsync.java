package io.github.jtpadilla.example.vertex.generativeai.example03;

import com.google.api.core.ApiFuture;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import io.github.jtpadilla.example.vertex.common.VertextaiUtil;
import io.github.jtpadilla.protobuf.ProtoJsonUtil;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

// Este ejemplo a diferencia de los anteriores utiliza los STUB asincronos java que genera el GRPC para
// poder consumir el resultado de forma asincrona.

public class TextGenerationWithAsync {

    static final private String MODEL_NAME = "gemini-2.0-flash";

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        try (VertexAI vertexAi = VertextaiUtil.vertexBuilder().build()) {

            final GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAi);

            final ApiFuture<GenerateContentResponse> future = model.generateContentAsync("How are you?");

            // Hacemos algo que resulte bloqueante...
            TimeUnit.SECONDS.sleep(4);

            // Get the response from Future
            final GenerateContentResponse response = future.get();

            // Seguro que tenemos el resultado ya preparado y si no espermos..
            System.out.println(ProtoJsonUtil.toJson(response));
        }

    }

}

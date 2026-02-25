package io.github.jtpadilla.example.vertex.generativeai.example09;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.cloud.vertexai.api.SafetySetting.HarmBlockThreshold;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import io.github.jtpadilla.example.vertex.common.VertextaiUtil;

import java.io.IOException;
import java.util.List;

// Construye la instancia de GenerativeModel mediante un builder el cual
// permite proporcioarle configuracion de seguridad.
// En este caso ademas utilizo el interface fluido para generar
// en una sola sentencia la respuesta.
public class RequestLevelConfigurations {

    static final private String MODEL_NAME = "gemini-2.0-flash";

    public static void main(String[] args) throws IOException {

        try (VertexAI vertexAi = VertextaiUtil.vertexBuilder().build()) {

            // Build a SafetySetting instance.
            final SafetySetting safetySetting = SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                .setThreshold(HarmBlockThreshold.BLOCK_LOW_AND_ABOVE)
                .build();

            final GenerateContentResponse response = new GenerativeModel.Builder()
                    .setVertexAi(vertexAi)
                    .setModelName(MODEL_NAME)
                    .setSafetySettings(List.of(safetySetting))
                    .build()
                    .generateContent("How are you?");

            System.out.println(ResponseHandler.getText(response));

        }
    }

}

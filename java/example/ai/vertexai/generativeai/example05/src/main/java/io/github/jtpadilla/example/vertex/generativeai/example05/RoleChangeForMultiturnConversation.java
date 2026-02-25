package io.github.jtpadilla.example.vertex.generativeai.example05;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import io.github.jtpadilla.example.vertex.common.VertextaiUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// Genera mediante la utilidad ContentMaker distintas instsnacias de
// del mensaje proto Content -> https://github.com/googleapis/googleapis/blob/master/google/ai/generativelanguage/v1/content.proto
// para envialos en el request del servicio GenerativeService -> https://github.com/googleapis/googleapis/blob/master/google/ai/generativelanguage/v1/generative_service.proto
// Simula manualmente que se ha tenido una conversacion previa como si de un chat se tratara..
public class RoleChangeForMultiturnConversation {

    static final private String MODEL_NAME = "gemini-2.0-flash";

    public static void main(String[] args) throws IOException {

        try (VertexAI vertexAi = VertextaiUtil.vertexBuilder().build()) {

            final GenerativeModel model =  new GenerativeModel(MODEL_NAME, vertexAi);

            // Put all the contents in a Content list
            final List<Content> contents = Arrays.asList(
                ContentMaker.fromString("Hi!"),
                ContentMaker.forRole("model") .fromString("Hello! How may I assist you?"),
                ContentMaker.fromString("Can you explain quantum mechanis as well in only a few sentences?")
            );

            // generate the result
            final GenerateContentResponse response = model.generateContent(contents);

            // ResponseHandler.getText is a helper function to retrieve the text part of the answer.
            System.out.println("\nPrint response: ");
            System.out.println(ResponseHandler.getText(response));
            System.out.println("\n");
        }
    }

}

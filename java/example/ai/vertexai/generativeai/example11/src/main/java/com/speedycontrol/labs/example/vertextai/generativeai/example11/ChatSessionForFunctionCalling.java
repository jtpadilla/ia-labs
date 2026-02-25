package com.speedycontrol.labs.example.vertextai.generativeai.example11;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.generativeai.*;
import com.speedycontrol.labs.example.vertextai.common.VertextaiUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

// Muestra como se implementa una llamada a funcion desde el modelo
public class ChatSessionForFunctionCalling {

    private static final String MODEL_NAME = "gemini-2.0-flash";

    private static final String JSON_STRING_FUNCTION = """
              {
                "name": "getCurrentWeather",
                "description": "Get the current weather in a given location",
                "filterLocationParameter": {
                  "type": "OBJECT",
                  "properties": {
                    "location": {
                      "type": "STRING",
                      "description": "location"
                    }
                  }
                }
              }
            """;

    private static final String PROMPT = "What's the weather in Vancouver?";

    public static void main(String[] args) throws IOException {

        try (VertexAI vertexAi = VertextaiUtil.vertexBuilder().build()) {

            // Declare a function to be used in a request.
            // We construct a jsonString that corresponds to the following function
            // declaration.
            final Tool tool = Tool.newBuilder()
                .addFunctionDeclarations(FunctionDeclarationMaker.fromJsonString(JSON_STRING_FUNCTION))
                .build();

            // Se cre el modelo con la funcion configurada
            final GenerativeModel model = new GenerativeModel.Builder()
                .setModelName(MODEL_NAME)
                .setVertexAi(vertexAi)
                .setTools(Arrays.asList(tool))
                .build();

            // Se crea el chat
            final ChatSession chat = model.startChat();

            // Peticion al modelo a traves del chat
            System.out.println(String.format("Ask the question: %s", PROMPT));
            GenerateContentResponse response = chat.sendMessage(PROMPT);
            System.out.println("Respuesta del modelo: ");
            System.out.println("===========================");
            System.out.println(ResponseHandler.getContent(response));
            System.out.println("\n");

            // Se le proporciona una respuesta al modelo para que este conozca el resultado de la "function call"
            final Content content = ContentMaker.fromMultiModalData(
                PartMaker.fromFunctionResponse("getCurrentWeather", Collections.singletonMap("currentWeather", "snowing"))
            );
            System.out.println("Le proporcionamos la respuesta: ");
            System.out.println(content);
            System.out.println("\n");
            response = chat.sendMessage(content);

            // See what the model replies now
            System.out.println("\nDe nuevo respuesta del modelo:: ");
            System.out.println(ResponseHandler.getText(response));
            System.out.println("\n");

        }
    }

}

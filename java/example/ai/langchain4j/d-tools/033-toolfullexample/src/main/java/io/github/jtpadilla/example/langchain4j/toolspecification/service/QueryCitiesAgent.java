package io.github.jtpadilla.example.langchain4j.toolspecification.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityListSchema;

public class QueryCitiesAgent {

    interface GeographyService {
        @SystemMessage("""
                Todos los prompts que recibirás estarán escritos en español y generarás
                todo el contenido también en español.

                Eres un experto en la geografía del país España.

                Responde ÚNICAMENTE con un objeto JSON válido con la siguiente estructura, sin texto adicional:
                {
                  "list": ["NombrePueblo1", "NombrePueblo2", ...]
                }
                """)
        String query(String userMessage);
    }

    public static CityListSchema call(ChatModel chatModel, String provincia) {
        GeographyService service = AiServices.create(GeographyService.class, chatModel);
        String response = service.query(
                String.format("Obtén la lista de los 10 pueblos con más población de la provincia de `%s`", provincia));
        return CityListSchema.fromJsonFlex(response);
    }

}

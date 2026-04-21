package io.github.jtpadilla.example.langchain4j.toolspecification.service.querycitiesdataagent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityDataListSchema;
import io.github.jtpadilla.example.util.SchemaException;

public class QueryCitiesDataAgent {

    interface WeatherService {
        @SystemMessage("""
                Todos los prompts que recibirás estarán escritos en español y generarás
                todo el contenido también en español.

                Eres un experto en clima del país España.

                Cuando proporciones fechas asegúrate de entregarlas sin zona horaria.

                Responde ÚNICAMENTE con un objeto JSON válido con la siguiente estructura, sin texto adicional:
                {
                  "list": [
                    {"city": "NombreCiudad", "localdatetime": "YYYY-MM-DDTHH:mm:ss", "temperature": 20.5},
                    ...
                  ]
                }
                """)
        String query(String userMessage);
    }

    public static CityDataListSchema call(ChatModel chatModel, String cityName) {
        WeatherService service = AiServices.create(WeatherService.class, chatModel);
        String userQuery = String.format("""
                Obtén la previsión de las próximas temperaturas aproximadamente en las horas en punto para la ciudad *%s*.
                Asegúrate que la fecha y hora estén en el formato ISO-8601 *AAAA-MM-DDThh:mm:ss*.
                """, cityName);
        String response = service.query(userQuery);
        try {
            return CityDataListSchema.fromJson(response);
        } catch (SchemaException e) {
            throw new RuntimeException("QueryCitiesDataAgent: error al parsear la respuesta para la ciudad " + cityName, e);
        }
    }

}

package io.github.jtpadilla.example.langchain4j.toolspecification.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityDataListSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.tool.GetCurrentTimeTool;
import io.github.jtpadilla.example.langchain4j.toolspecification.tool.FilterLocationsTool;
import io.github.jtpadilla.example.util.SchemaException;

import java.util.List;
import java.util.stream.Collectors;

public class FilterAgent {

    interface FilterService {
        @SystemMessage("""
                Todos los prompts que recibirás estarán escritos en español y generarás
                todo el contenido también en español.

                Eres una herramienta especializada en filtrar datos estructurados.
                Utiliza las herramientas que te han proporcionado para poder realizar estas operaciones.

                Cuando proporciones fechas asegúrate de entregarlas sin zona horaria.

                Responde ÚNICAMENTE con un objeto JSON válido con la siguiente estructura, sin texto adicional:
                {
                  "list": [
                    {"city": "NombreCiudad", "localdatetime": "YYYY-MM-DDTHH:mm:ss", "temperature": 20.5},
                    ...
                  ]
                }
                """)
        String filter(String userMessage);
    }

    public static CityDataListSchema call(ChatModel chatModel, List<CityDataListSchema> rawData, List<String> ciudades) {

        GetCurrentTimeTool currentTimeTool = new GetCurrentTimeTool();
        FilterLocationsTool filterLocationsTool = new FilterLocationsTool(ciudades);

        FilterService service = AiServices.builder(FilterService.class)
                .chatModel(chatModel)
                .tools(currentTimeTool, filterLocationsTool)
                .build();

        String allData = rawData.stream()
                .map(CityDataListSchema::toJson)
                .collect(Collectors.joining("\n"));

        String userPrompt = String.format("""
                Eres un asistente experto en procesar datos de usuarios.

                Hay que filtrar los datos que has recibido realizando los siguientes pasos:

                1. Sumariza la lista de ciudades que hay en los datos.
                2. Utiliza la lista de ciudades que has sumarizado para pasarla a la herramienta y que esta te indique en cuales estamos interesados.
                3. Filtra los datos de entrada quitando los de las ciudades que no nos interesan.
                4. De los datos resultantes deja únicamente para cada pueblo el que tiene la máxima temperatura.
                5. La lista resultante es el resultado final que me interesa.

                A continuación, todos los datos que tienes que procesar:
                %s
                """, allData);

        String response = service.filter(userPrompt);
        try {
            return CityDataListSchema.fromJson(response);
        } catch (SchemaException e) {
            throw new RuntimeException("FilterAgent: error al parsear la respuesta del LLM como CityDataListSchema", e);
        }
    }

}

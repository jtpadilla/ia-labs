package io.github.jtpadilla.example.langchain4j.toolspecification;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityDataListSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityListSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.FilterAgent;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.QueryCitiesDataAgent;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.QueryCitiesAgent;
import io.github.jtpadilla.example.util.GoogleModels;
import io.helidon.config.Config;

import java.util.List;

public class ToolDemo {

    private static final String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("La clave de configuración 'gemini-api-key' es obligatoria"));

    static final ChatModel chatModel = GoogleAiGeminiChatModel.builder()
            .apiKey(API_KEY)
            .modelName(GoogleModels.geminiFlashLite())
            .build();

    public static void main(String[] args) {

        final String provincia = "Sevilla";
        final List<String> ciudadesInteres = List.of("Sevilla", "Dos Hermanas", "Alcalá de Guadaíra");

        // 1. Obtiene la lista de ciudades de la provincia usando AI Services
        System.out.println("=== Consultando ciudades de " + provincia + " ===");
        CityListSchema cities = QueryCitiesAgent.call(chatModel, provincia);
        System.out.println("Ciudades encontradas: " + cities.getList());

        // 2. Consulta los datos meteorológicos de cada ciudad usando AI Services
        System.out.println("\n=== Consultando datos meteorológicos ===");
        List<CityDataListSchema> rawData = cities.getList().stream()
                .map(city -> {
                    System.out.println("  Consultando: " + city);
                    return QueryCitiesDataAgent.call(chatModel, city);
                })
                .toList();

        // 3. Filtra y procesa los datos usando AI Services + @Tool
        System.out.println("\n=== Filtrando datos con AI Services + @Tool ===");
        CityDataListSchema filtered = FilterAgent.call(chatModel, rawData, ciudadesInteres);

        System.out.println("\n=== Resultado Final ===");
        System.out.println(filtered.toJson());
    }

}

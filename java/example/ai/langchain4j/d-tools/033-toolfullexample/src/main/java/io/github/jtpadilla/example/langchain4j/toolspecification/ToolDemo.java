package io.github.jtpadilla.example.langchain4j.toolspecification;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.langchain4j.toolspecification.domain.TemperatureEntry;
import io.github.jtpadilla.example.langchain4j.toolspecification.domain.TemperatureQueryException;
import io.github.jtpadilla.example.langchain4j.toolspecification.domain.TemperatureQueryResult;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityDataListSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityListSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.Filter;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.QueryCities;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.QueryCitiesData;
import io.github.jtpadilla.example.util.GoogleModels;
import io.helidon.config.Config;

import java.util.ArrayList;
import java.util.List;

public class ToolDemo {

    private static final String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("La clave de configuración 'gemini-api-key' es obligatoria"));

    // Con Google Search para las consultas de datos
    static final ChatModel chatModelSearch = GoogleAiGeminiChatModel.builder()
            .apiKey(API_KEY)
            .modelName(GoogleModels.geminiFlashLite())
            .allowGoogleSearch(true)
            .build();

    // Sin Google Search para pasos que usan function calling personalizado
    static final ChatModel chatModelNoSearch = GoogleAiGeminiChatModel.builder()
            .apiKey(API_KEY)
            .modelName(GoogleModels.geminiFlashLite())
            .build();

    public static void main(String[] args) {
        try {
            final TemperatureQueryResult result = query("Castellón", List.of("Burriana"));
            System.out.println(result);
        } catch (TemperatureQueryException e) {
            throw new RuntimeException(e);
        }
    }

    static public TemperatureQueryResult query(String provincia, List<String> ciudades) throws TemperatureQueryException {
        try {
            // Obtenemos la lista de ciudades con mas poblacion de la provincia proporcionada como parámetro
            CityListSchema cityList = QueryCities.call(chatModelSearch, provincia);
            System.out.println("[Lista de ciudades]");
            System.out.println(cityList.toJson());

            // Para cada una de las ciudades obtenemos la prevision de temperaturas
            final List<CityDataListSchema> dataList = new ArrayList<>();
            for (String cityName : cityList.getList()) {
                CityDataListSchema cityDataList = QueryCitiesData.call(chatModelSearch, cityName);
                dataList.add(cityDataList);
                System.out.format("[Temperaturas para %s%n]", cityName);
                System.out.println(cityDataList.toJson());
            }

            // Finalmente, filtramos las lecturas que sean de determinadas ciudades y
            // únicamente la más alta para cada ciudad.
            final CityDataListSchema result = Filter.call(chatModelNoSearch, dataList, ciudades);

            // Se convierte el resultado
            return new TemperatureQueryResult(
                    result.getList().stream()
                            .map(entry -> new TemperatureEntry(entry.localDateTime(), entry.city(), entry.temperature()))
                            .toList()
            );
        } catch (RuntimeException e) {
            throw new TemperatureQueryException(e.getMessage(), e);
        }
    }

}

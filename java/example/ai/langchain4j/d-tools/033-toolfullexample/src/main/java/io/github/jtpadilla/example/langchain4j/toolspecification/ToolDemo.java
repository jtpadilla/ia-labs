package io.github.jtpadilla.example.langchain4j.toolspecification;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityDataListSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityListSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.impl.TemperatureEntry;
import io.github.jtpadilla.example.langchain4j.toolspecification.impl.TemperatureQueryException;
import io.github.jtpadilla.example.langchain4j.toolspecification.impl.TemperatureQueryResult;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.FilterAgent;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.QueryCitiesDataAgent;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.QueryCitiesAgent;
import io.github.jtpadilla.example.util.GoogleModels;
import io.helidon.config.Config;

import java.util.ArrayList;
import java.util.List;

public class ToolDemo {

    private static final String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("La clave de configuración 'gemini-api-key' es obligatoria"));

    static final ChatModel chatModel = GoogleAiGeminiChatModel.builder()
            .apiKey(API_KEY)
            .modelName(GoogleModels.geminiFlashLite())
            .allowGoogleSearch(true)
            .build();

    static final ToolDemo service = new ToolDemo();

    public static void main(String[] args) {
        try {
            final TemperatureQueryResult result = service.query(
                    "Castellón",
                    List.of("Burriana")
            );
            System.out.println(result);
        } catch (TemperatureQueryException e) {
            throw new RuntimeException(e);
        }

    }

    public TemperatureQueryResult query(String provincia, List<String> ciudades) throws TemperatureQueryException {
        try {
            // Obtenemos la lista de ciudades con mas poblacion de la provincia proporcionada como parámetro
            CityListSchema cityList = QueryCitiesAgent.call(chatModel, provincia);
            System.out.println("[Lista de ciudades]");
            System.out.println(cityList.toJson());

            // Para cada una de las ciudades obtenemos la prevision de temperaturas
            final List<CityDataListSchema> dataList = new ArrayList<>();
            for (String cityName : cityList.getList()) {
                CityDataListSchema cityDataList = QueryCitiesDataAgent.call(chatModel, cityName);
                dataList.add(cityDataList);
                System.out.format("[Temperaturas para %s%n]", cityName);
                System.out.println(cityDataList.toJson());
            }

            // Finalmente, filtramos las lecturas que sean de determinadas ciudades y
            // únicamente la más alta para cada ciudad.
            final CityDataListSchema result = FilterAgent.call(chatModel, dataList, ciudades);

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

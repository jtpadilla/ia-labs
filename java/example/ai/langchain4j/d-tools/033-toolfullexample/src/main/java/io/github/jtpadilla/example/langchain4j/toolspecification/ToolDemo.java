package io.github.jtpadilla.example.langchain4j.toolspecification;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.langchain4j.toolspecification.impl.TemperatureQueryService;
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
            final TemperatureQueryResult result = TemperatureQueryService.query(
                    chatModel,
                    "Castellón",
                    List.of("Burriana")
            );
            System.out.println(result);
        } catch (TemperatureQueryException e) {
            throw new RuntimeException(e);
        }

    }


}

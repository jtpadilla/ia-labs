package io.github.jtpadilla.example.langchain4j.toolspecification;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.util.GoogleModels;
import io.helidon.config.Config;

import java.util.ArrayList;
import java.util.List;

public class ToolDemo {

    private static final String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("La clave de configuración 'gemini-api-key' es obligatoria"));

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(GoogleModels.geminiFlashLite())
                .build();

        ToolSpecification toolSpecification = ToolSpecification.builder()
                .name("getWeather")
                .description("Returns the weather forecast for a given city")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("city", "The city for which the weather forecast should be returned")
                        .addEnumProperty("temperatureUnit", List.of("CELSIUS", "FAHRENHEIT"))
                        .required("city") // the required properties should be specified explicitly
                        .build())
                .build();

        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from("What will the weather be like in London tomorrow?"))
                .toolSpecifications(List.of(toolSpecification))
                .build();

        // Round 1: model responds with tool execution requests
        ChatResponse response = chatModel.chat(request);
        AiMessage aiMessage = response.aiMessage();

        System.out.println("Has tool requests: " + aiMessage.hasToolExecutionRequests());

        // Round 2: execute each requested tool and send results back
        List<ToolExecutionResultMessage> toolResults = aiMessage.toolExecutionRequests().stream()
                .map(toolExecutionRequest -> {
                    System.out.println("Tool called: " + toolExecutionRequest.name());
                    System.out.println("Arguments  : " + toolExecutionRequest.arguments());
                    String result = WeatherTool.execute(toolExecutionRequest.arguments());
                    return ToolExecutionResultMessage.from(toolExecutionRequest, result);
                })
                .toList();

        UserMessage userMessage = UserMessage.from("What will the weather be like in London tomorrow?");

        List<ChatMessage> messages2 = new ArrayList<>();
        messages2.add(userMessage);
        messages2.add(aiMessage);
        messages2.addAll(toolResults);

        ChatRequest request2 = ChatRequest.builder()
                .messages(messages2)
                .toolSpecifications(List.of(toolSpecification))
                .build();

        ChatResponse response2 = chatModel.chat(request2);
        System.out.println(response2.aiMessage().text());

    }

}

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

    static final ChatModel chatModel = GoogleAiGeminiChatModel.builder()
            .apiKey(API_KEY)
            .modelName(GoogleModels.geminiFlashLite())
            .sendThinking(true)
            .build();

    static final ToolSpecification toolSpecification = ToolSpecification.builder()
            .name("getWeather")
            .description("Returns the weather forecast for a given city")
            .parameters(JsonObjectSchema.builder()
                    .addStringProperty("city", "The city for which the weather forecast should be returned")
                    .addEnumProperty("temperatureUnit", List.of("CELSIUS", "FAHRENHEIT"))
                    .required("city") // the required properties should be specified explicitly
                    .build())
            .build();

    static final UserMessage firstMessage = UserMessage.from("What will the weather be like in London tomorrow?");

    public static void main(String[] args) {
        ChatResponse firstChatResponse = sendFirstRequest();
        List<ToolExecutionResultMessage> toolExecutionResultMessages = executeTools(firstChatResponse);
        ChatResponse secondChatResponse = sendSecondRequest(firstChatResponse.aiMessage(), toolExecutionResultMessages);
        System.out.println(secondChatResponse.aiMessage().text());
    }

    static private ChatResponse sendFirstRequest() {

        final ChatRequest request = ChatRequest.builder()
                .messages(firstMessage)
                .toolSpecifications(List.of(toolSpecification))
                .build();

        return chatModel.chat(request);
    }

    static private List<ToolExecutionResultMessage> executeTools(ChatResponse chatResponse) {
        final AiMessage aiMessage = chatResponse.aiMessage();
        System.out.println("Hay petición de herramienta?: " + aiMessage.hasToolExecutionRequests());
        return aiMessage.toolExecutionRequests().stream()
                .map(toolExecutionRequest -> {
                    System.out.println("Herramienta invocada: " + toolExecutionRequest.name());
                    System.out.println("Argumentos          : " + toolExecutionRequest.arguments());
                    String result = WeatherTool.execute(toolExecutionRequest.arguments());
                    System.out.println("Resultado           : " + result);
                    return ToolExecutionResultMessage.from(toolExecutionRequest, result);
                })
                .toList();
    }

    static private ChatResponse sendSecondRequest(AiMessage firstResponseAi, List<ToolExecutionResultMessage> toolResults) {

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(firstMessage);
        messages.add(firstResponseAi);
        messages.addAll(toolResults);

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(List.of(toolSpecification))
                .build();

        return chatModel.chat(request);

    }

}

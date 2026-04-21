package io.github.jtpadilla.example.langchain4j.toolfullexample.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
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
import io.github.jtpadilla.example.langchain4j.toolfullexample.WeatherTool;
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
            .returnThinking(true)
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

        // Primera petición con su respuesta
        ChatResponse firstChatResponse = sendFirstRequest();

        // Se procesa con la primera respuesta la herramienta solicitada
        List<ToolExecutionResultMessage> toolExecutionResultMessages = executeTools(firstChatResponse);

        // Segunda petition con la primera pregunta + primera respuesta + resultado herramienta solicitada.
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

        final List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
        System.out.println("Hay peticiónes de herramientas?: " + aiMessage.hasToolExecutionRequests());

        if (toolExecutionRequests.size() == 1 && toolExecutionRequests.getFirst().name().equals("getWeather")) {
            ToolExecutionRequest toolExecutionRequest = toolExecutionRequests.getFirst();
            System.out.println("Herramienta invocada: " + toolExecutionRequest.name());
            System.out.println("Argumentos          : " + toolExecutionRequest.arguments());
            String result = WeatherTool.execute(toolExecutionRequest.arguments());
            System.out.println("Resultado           : " + result);
            final ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, result);
            return List.of(resultMessage);
        } else {
            throw new IllegalStateException("No esta la tool solicitada disponible.");
        }

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

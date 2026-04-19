package io.github.jtpadilla.example.langchain4j.aiservices6;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import io.helidon.config.Config;

public class AiServicesDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true) // Útil para debug en Bazel
                .build();

        interface AssistantWithChatParams {
            String chat(@UserMessage String userMessage, ChatRequestParameters params);
        }

        AssistantWithChatParams friend = AiServices.builder(AssistantWithChatParams.class)
                .chatModel(chatModel)
                .build();

        ChatRequestParameters customParams = ChatRequestParameters.builder()
                .temperature(0.85)
                .build();

        String answer = friend.chat("Hola", customParams);
        System.out.println(answer); // Hello, how can I help you?

    }

}
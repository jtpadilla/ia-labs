package io.github.jtpadilla.example.langchain4j.aiservices3;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import io.helidon.config.Config;

import java.time.LocalDate;

public class AiServicesDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true) // Útil para debug en Bazel
                .build();

        interface Friend {
            String chat(String userMessage);
        }

        Friend friend = AiServices.builder(Friend.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> "You are a good friend of mine.")
                .systemMessageTransformer(systemMessage -> systemMessage + " Today's date is " + LocalDate.now() + ".")
                .build();

        String answer = friend.chat("Hola, que hora es?");
        System.out.println(answer); // Hello, how can I help you?

    }

}
package io.github.jtpadilla.example.langchain4j.aiservices2;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import io.helidon.config.Config;

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
            @SystemMessage("You are a good friend of mine. Answer using slang and using Spanish")
            String chat(String userMessage);
        }

        Friend friend = AiServices.create(Friend.class, model);

        String answer = friend.chat("Hola");
        System.out.println(answer); // Hello, how can I help you?

    }

}
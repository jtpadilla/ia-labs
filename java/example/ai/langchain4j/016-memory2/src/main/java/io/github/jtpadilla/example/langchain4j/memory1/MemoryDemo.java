package io.github.jtpadilla.example.langchain4j.memory1;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.service.*;
import io.helidon.config.Config;

import java.util.concurrent.CompletableFuture;

public class MemoryDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        interface Assistant {
            @SystemMessage("Answer always in Spanish")
            String chat(@UserMessage String message);
        }

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        String answerToKlaus = assistant.chat("Hello, my name is Klaus");
        System.out.println(answerToKlaus); // Hello, how can I help you?

        String answerToFrancine = assistant.chat("Hola, mi nombre era Francine o era otro?");
        System.out.println(answerToFrancine); // Hello, how can I help you?

    }

}
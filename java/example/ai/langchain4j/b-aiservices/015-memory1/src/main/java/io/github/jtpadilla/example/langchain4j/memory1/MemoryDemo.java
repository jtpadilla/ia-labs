package io.github.jtpadilla.example.langchain4j.memory1;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.helidon.config.Config;

public class MemoryDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        interface Assistant {
            @SystemMessage("Answer always in Spanish")
            String chat(@UserMessage String message);
        }

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        String answerToKlaus = assistant.chat("Hello, my name is Klaus");
        System.out.println(answerToKlaus); // Hello, how can I help you?

        String answerToFrancine = assistant.chat("Hola, mi nombre era Francine o era otro?");
        System.out.println(answerToFrancine); // Hello, how can I help you?

    }

}
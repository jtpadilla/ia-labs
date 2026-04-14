package io.github.jtpadilla.example.langchain4j.sequentialworkflow;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import io.helidon.config.Config;

public class AgentDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        interface Assistant  {
            String chat(@MemoryId int memoryId, @UserMessage String message);
        }

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        String answerToKlaus = assistant.chat(1,"Hola, mi nombre es Klaus");
        System.out.println("1) " + answerToKlaus); // Hello, how can I help you?

        String answerToFrancine = assistant.chat(2, "Hola, mi nombre es Francine");
        System.out.println("2) " + answerToFrancine); // Hello, how can I help you?

        answerToKlaus = assistant.chat(1,"Cual es mi nombre?");
        System.out.println("1) " + answerToKlaus); // Hello, how can I help you?

        answerToFrancine = assistant.chat(2, "Cual es mi nombre?");
        System.out.println("2) " + answerToFrancine); // Hello, how can I help you?

    }

}
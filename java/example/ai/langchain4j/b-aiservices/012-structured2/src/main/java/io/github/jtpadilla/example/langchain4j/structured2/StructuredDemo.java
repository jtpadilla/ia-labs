package io.github.jtpadilla.example.langchain4j.structured2;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import io.helidon.config.Config;

public class StructuredDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        enum Priority {
            CRITICAL, HIGH, LOW
        }

        interface PriorityAnalyzer {
            @UserMessage("Analyze the priority of the following issue: {{it}}")
            Priority analyzePriority(String issueDescription);
        }

        PriorityAnalyzer priorityAnalyzer = AiServices.create(PriorityAnalyzer.class, chatModel);

        Priority priority = priorityAnalyzer.analyzePriority("The main payment gateway is down, and customers cannot process transactions.");
        System.out.println(priority); // Hello, how can I help you?

    }

}
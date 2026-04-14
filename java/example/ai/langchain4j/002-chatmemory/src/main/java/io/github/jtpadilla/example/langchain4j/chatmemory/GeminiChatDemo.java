package io.github.jtpadilla.example.langchain4j.chatmemory;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import io.helidon.config.Config;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

public class GeminiChatDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        final ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);

        final ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true) // Útil para debug en Bazel
                .build();

        final ConversationalChain chain = ConversationalChain.builder()
                .chatModel(model)
                .chatMemory(chatMemory)
                .build();

        String answer = chain.execute("What are all the movies directed by Quentin Tarantino?");
        System.out.println(answer); // Pulp Fiction, Kill Bill, etc.

        answer = chain.execute("How old is he?");
        System.out.println(answer); // Quentin Tarantino was born on March 27, 1963, so he is currently 58 years old.

    }

}
package io.github.jtpadilla.example.langchain4j.chatmemory1;

import io.helidon.config.Config;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

public class ChatMemory {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true) // Útil para debug en Bazel
                .build();

        // Primera interacción
        String answer = model.chat("List 3 movies by Quentin Tarantino.");
        System.out.println("Gemini: " + answer);

        // Segunda interacción (Sin memoria explícita, Gemini no sabrá quién es "he")
        // Para que funcione igual que tu demo, especificamos el sujeto:
        String followUp = model.chat("How old is he?");
        System.out.println("Gemini: " + followUp);

    }

}
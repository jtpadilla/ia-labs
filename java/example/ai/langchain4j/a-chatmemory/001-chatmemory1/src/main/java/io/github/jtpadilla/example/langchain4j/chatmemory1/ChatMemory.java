package io.github.jtpadilla.example.langchain4j.chatmemory1;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.util.Format;
import io.helidon.config.Config;

public class ChatMemory {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true) // Útil para debug en Bazel
                .build();

        // Primera interacción
        String answer = chatModel.chat("List 3 movies by Quentin Tarantino.");
        System.out.println(Format.markdown(answer));

        // Segunda interacción (Sin memoria explícita, Gemini no sabrá quién es "he")
        // Para que funcione igual que tu demo, especificamos el sujeto:
        String followUp = chatModel.chat("How old is he?");
        System.out.println(Format.markdown(followUp));

    }

}
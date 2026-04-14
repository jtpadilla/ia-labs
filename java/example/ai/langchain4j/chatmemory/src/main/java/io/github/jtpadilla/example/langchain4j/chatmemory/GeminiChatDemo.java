package io.github.jtpadilla.example.langchain4j.chatmemory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

public class GeminiChatDemo {

    public static void main(String[] args) {
        // En un entorno real, usa System.getenv("GOOGLE_AI_KEY")
        String apiKey = "TU_API_KEY_AQUI";

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-flash")
                .logRequestsAndResponses(true) // Útil para debug en Bazel
                .build();

        // Primera interacción
        String answer = model.chat("List 3 movies by Quentin Tarantino.");
        System.out.println("Gemini: " + answer);

        // Segunda interacción (Sin memoria explícita, Gemini no sabrá quién es "he")
        // Para que funcione igual que tu demo, especificamos el sujeto:
        String followUp = model.chat("How old is Quentin Tarantino?");
        System.out.println("Gemini: " + followUp);
    }
}
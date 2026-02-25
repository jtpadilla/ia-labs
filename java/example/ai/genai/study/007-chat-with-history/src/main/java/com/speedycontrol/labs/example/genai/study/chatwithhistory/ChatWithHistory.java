package com.speedycontrol.labs.example.genai.study.chatwithhistory;

import com.google.genai.Chat;
import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

import java.util.List;

public class ChatWithHistory {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            // Se crea una sesion de chat
            final Chat chatSession = client.chats.create(genAIService.getLlmModel());

            // Se obtiene LA respuesta mediante el metodo rapido 'text()'
            final GenerateContentResponse response = chatSession.sendMessage("Puedes contarme la historia del queso en 100 palabras?");
            System.out.println("Unary response: " + response.text());

            // Continuando con el chat
            final GenerateContentResponse response2 = chatSession.sendMessage("Puedes modificar la historia para un niño de 5 años?");
            System.out.println("Unary response: " + response2.text());

            // Get the history of the chat session.
            // Passing 'true' to getHistory() returns the curated history, which excludes empty or invalid parts.
            // Passing 'false' here would return the comprehensive history, including empty or invalid parts.
            System.out.println();
            System.out.println("[Historia]");
            dumpContent(chatSession.getHistory(true));

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

    static private void dumpContent(List<Content> contentList) {
        contentList.forEach(ChatWithHistory::dumpContent);
    }

    static private void dumpContent(Content content) {
        System.out.format("%s: %s%n", content.role().orElse("EmptyRole"), content.text());
    }

}

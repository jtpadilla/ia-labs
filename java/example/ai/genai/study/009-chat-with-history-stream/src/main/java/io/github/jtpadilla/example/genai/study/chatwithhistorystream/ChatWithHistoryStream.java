package io.github.jtpadilla.example.genai.study.chatwithhistorystream;

import com.google.genai.Chat;
import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.gcloud.genai.IGenAIService;

import java.util.List;

public class ChatWithHistoryStream {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            // Se crea una sesion de chat
            final Chat chatSession = client.chats.create(genAIService.getLlmModel());

            // Interaccion 1
            final ResponseStream<GenerateContentResponse> responseStream = chatSession.sendMessageStream(
                    "Puedes contarme la historia del queso en 100 palabras?",
                    null
            );
            System.out.println("Streaming response:");
            for (GenerateContentResponse response : responseStream) {
                // Iterate over the stream and print each response as it arrives.
                System.out.print(response.text());
            }
            System.out.println();

            // Interaccion 2
            ResponseStream<GenerateContentResponse> responseStream2 = chatSession.sendMessageStream(
                    "Puedes modificar la historia para un niño de 5 años?",
                    null
            );
            System.out.println("Streaming response 2:");
            for (GenerateContentResponse response : responseStream2) {
                // Iterate over the stream and print each response as it arrives.
                System.out.print(response.text());
            }
            System.out.println();

            System.out.println();
            System.out.println("[Historia]");
            // Get the history of the chat session.
            // History is added after the stream is consumed and includes the aggregated response from the
            // stream, so chatSession.getHistory(false) here returns 4 items (2 user-model message pairs)
            dumpContent(chatSession.getHistory(true));

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

    static private void dumpContent(List<Content> contentList) {
        contentList.forEach(ChatWithHistoryStream::dumpContent);
    }

    static private void dumpContent(Content content) {
        System.out.format("%s: %s%n", content.role().orElse("EmptyRole"), content.text());
    }

}

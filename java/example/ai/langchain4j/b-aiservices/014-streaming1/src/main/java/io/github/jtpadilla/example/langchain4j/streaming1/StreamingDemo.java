package io.github.jtpadilla.example.langchain4j.streaming1;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import io.helidon.config.Config;

import java.util.concurrent.CompletableFuture;

public class StreamingDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        StreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        interface Assistant {
            TokenStream chat(String message);
        }

        Assistant assistant = AiServices.create(Assistant.class, model);

        TokenStream tokenStream = assistant.chat("Tell me a long joke in Spanish");

        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        tokenStream
                .onPartialResponse(System.out::println)
                .onPartialThinking(System.out::println)
                .onRetrieved(System.out::println)
                .onIntermediateResponse(System.out::println)
                // This will be invoked every time a new partial tool call (usually containing a single token of the tool's arguments) is available.
                .onPartialToolCall(System.out::println)
                // This will be invoked right before a tool is executed. BeforeToolExecution contains ToolExecutionRequest (e.g. tool name, tool arguments, etc.)
                .beforeToolExecution(System.out::println)
                // This will be invoked right after a tool is executed. ToolExecution contains ToolExecutionRequest and tool execution result.
                .onToolExecuted(System.out::println)
                .onCompleteResponse(futureResponse::complete)
                .onError(futureResponse::completeExceptionally)
                .start();

        // .join() bloquea la ejecución y devuelve el resultado cuando está listo.
        ChatResponse response = futureResponse.join();

        // Ahora podemos usar el resultado final para obtener metadatos.
        System.out.println("\n\n--- Streaming Complete ---");
        System.out.println("Finish Reason: " + response.finishReason());
        System.out.println("Token Usage: " + response.tokenUsage());
    }

}
package com.speedycontrol.labs.example.genai.study.charwithfunctioncall;

import com.google.genai.Chat;
import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Tool;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

import java.lang.reflect.Method;
import java.util.List;

public class ChatWithFunctionCall {

    public static String getCurrentWeather(String location) {
        return "The weather in " + location + " is " + "very nice.";
    }

    public static Integer divideTwoIntegers(int numerator, int denominator) {
        return numerator / denominator;
    }

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            final Method method1 = ChatWithFunctionCall.class.getDeclaredMethod("getCurrentWeather", String.class);
            final Method method2 = ChatWithFunctionCall.class.getDeclaredMethod("divideTwoIntegers", int.class, int.class);

            // Se preparan las dos funciones
            final Tool tool = Tool.builder()
                    .functions(method1, method2)
                    .build();

            // Se incorporan en la configuracion del chat
            final GenerateContentConfig config = GenerateContentConfig.builder()
                    .tools(tool)
                    .build();

            // Create a chat session.
            final Chat chatSession = client.chats.create(genAIService.getLlmModel(), config);

            System.out.println();

            final GenerateContentResponse response1 = chatSession
                    .sendMessage("what is the weather in San Francisco?");
            System.out.println("first response: " + response1.text());
            System.out.println();

            final GenerateContentResponse response2 = chatSession
                    .sendMessage("can you divide 10 by 2?");
            System.out.println("second response: " + response2.text());


            System.out.println();
            System.out.println("[chat history]");
            dumpContent(chatSession.getHistory(false));
            System.out.println();

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    static private void dumpContent(List<Content> contentList) {
        contentList.forEach(ChatWithFunctionCall::dumpContent);
    }

    static private void dumpContent(Content content) {
        System.out.format("%s: %s%n", content.role().orElse("EmptyRole"), content.text());
    }

}

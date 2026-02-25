package com.speedycontrol.labs.example.genai.study.generatecontentwithfunctioncall;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Tool;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

import java.lang.reflect.Method;

public class GenerateContentWithFunctionCall {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();
        try (Client client = genAIService.createClient()) {

            // Load the two methods as reflected Method objects so that they can be automatically executed on the client side.
            final Method method1 = GenerateContentWithFunctionCall.class.getMethod(
                    "getCurrentWeather",
                    String.class,
                    String.class
            );
            final Method method2 = GenerateContentWithFunctionCall.class.getMethod(
                    "divideTwoIntegers",
                    int.class,
                    int.class
            );

            // Add the two methods as callable functions to the list of tools.
            final GenerateContentConfig config = GenerateContentConfig.builder()
                    .tools(Tool.builder().functions(method1, method2))
                    .build();

            final GenerateContentResponse response = client.models.generateContent(
                    genAIService.getLlmModel(),
                    "What is the weather in Vancouver? And can you divide 10 by 0?",
                    config
            );

            System.out.println("The response is: " + response.text());
            System.out.println("The automatic function calling history is: " + response.automaticFunctionCallingHistory().get());

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    /** A callable function to get the weather. */
    public static String getCurrentWeather(String location, String unit) {
        return "The weather in " + location + " is " + "very nice.";
    }

    /** A callable function to divide two integers. */
    public static Integer divideTwoIntegers(int numerator, int denominator) {
        return numerator / denominator;
    }

}

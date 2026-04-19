package io.github.jtpadilla.example.langchain4j.structured3;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import io.helidon.config.Config;

import java.time.LocalDate;

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

        @Description("an address") // you can add an optional description to help an LLM have a better understanding
        record Address(
            String street,
            Integer streetNumber,
            String city) {}

        record Person (
            @Description("first name of a person") // you can add an optional description to help an LLM have a better understanding
            String firstName,
            String lastName,
            LocalDate birthDate,
            Address address) {}

        interface PersonExtractor {
            @UserMessage("Extract information about a person from {{it}}")
            Person extractPersonFrom(String text);
        }

        PersonExtractor personExtractor = AiServices.create(PersonExtractor.class, chatModel);

        String text = """
            In 1968, amidst the fading echoes of Independence Day,
            a child named John arrived under the calm evening sky.
            This newborn, bearing the surname Doe, marked the start of a new journey.
            He was welcomed into the world at 345 Whispering Pines Avenue
            a quaint street nestled in the heart of Springfield
            an abode that echoed with the gentle hum of suburban dreams and aspirations.
            """;

        Person person = personExtractor.extractPersonFrom(text);

        System.out.println(person); // Hello, how can I help you?

    }

}
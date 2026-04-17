package io.github.jtpadilla.example.langchain4j.parallelmapperworkflow1;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.jtpadilla.example.util.Format;
import io.helidon.config.Config;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgentDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public record Person(String name, String horoscope) {}

    public interface PersonAstrologyAgent {
        @SystemMessage("""
        Eres un astrólogo que genera horóscopos basándose en el nombre y el signo zodiacal del usuario.
        """)
        @UserMessage("""
        Genera el horóscopo para {{person}}.
        La persona tiene un nombre y un signo zodiacal. Usa ambos para crear un horóscopo personalizado.
        """)
        @Agent(description = "Un astrólogo que genera horóscopos para una persona", outputKey = "horoscope")
        String horoscope(@V("person") Person person);
    }

    public interface BatchHoroscopeAgent extends AgentInstance {
        @Agent
        List<String> generateHoroscopes(@V("persons") List<Person> persons);
    }

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        PersonAstrologyAgent personAstrologyAgent = AgenticServices
                .agentBuilder(PersonAstrologyAgent.class)
                .chatModel(chatModel)
                .outputKey("horoscope")
                .build();

        try (ExecutorService executorService = Executors.newFixedThreadPool(3)) {
            BatchHoroscopeAgent agent = AgenticServices
                    .parallelMapperBuilder(BatchHoroscopeAgent.class)
                    .subAgents(personAstrologyAgent)
                    .itemsProvider("persons")
                    .executor(executorService)
                    .build();

            List<Person> persons = List.of(
                    new Person("Mario", "aries"),
                    new Person("Luigi", "pisces"),
                    new Person("Peach", "leo"));

            List<String> horoscopes = agent.generateHoroscopes(persons);

            horoscopes.stream().map(Format::markdown).forEach(System.out::println);

        }

    }

}
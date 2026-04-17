package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.web.search.WebSearchInformationResult;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.WebSearchTool;
import io.github.jtpadilla.example.util.Format;
import io.github.jtpadilla.example.util.GoogleModels;
import io.helidon.config.Config;

import java.net.URI;
import java.util.List;

public class AgentDemo {

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public enum Sign {
        ARIES,
        TAURO,
        GEMINIS,
        CANCER,
        LEO,
        VIRGO,
        LIBRA,
        ESCORPIO,
        SAGITARIO,
        CAPRICORNIO,
        ACUARIO,
        PISCIS
    }

    public record Person(String name, String horoscope) {}

    public interface HoroscopeGenerator {
        @SystemMessage("You are an astrologist that generates horoscopes based on the user's name and zodiac sign.")
        @UserMessage("Generate the horoscope for {{person}} who is a {{sign}}.")
        @Agent("An astrologist that generates horoscopes based on the user's name and zodiac sign.")
        String horoscope(@V("person") Person person, @V("sign") Sign sign);
    }

    public interface PersonExtractor {

        @UserMessage("Extract a person from the following prompt: {{prompt}}")
        @Agent("Extract a person from user's prompt")
        Person extractPerson(@V("prompt") String prompt);
    }

    public interface SignExtractor {

        @UserMessage("Extract the zodiac sign of a person from the following prompt: {{prompt}}")
        @Agent("Extract a person from user's prompt")
        Sign extractSign(@V("prompt") String prompt);
    }

    public interface Writer {
        @UserMessage("""
            Create an amusing writeup for {{person}} based on the following:
            - their horoscope: {{horoscope}}
            - a current news story: {{story}}
            """)
        @Agent("Create an amusing writeup for the target person based on their horoscope and current news stories")
        String write(@V("person") Person person, @V("horoscope") String horoscope, @V("story") String story);
    }

    public interface StoryFinder {

        @SystemMessage("""
            You're a story finder, use the provided web search tools, calling it once and only once,
            to find a fictional and funny story on the internet about the user provided topic.
            """)
        @UserMessage("""
            Find a story on the internet for {{person}} who has the following horoscope: {{horoscope}}.
            """)
        @Agent("Find a story on the internet for a given person with a given horoscope")
        String findStory(@V("person") Person person, @V("horoscope") String horoscope);
    }

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(GoogleModels.geminiFlashLite())
                .logRequestsAndResponses(true)
                .sendThinking(true)
                .returnThinking(true)
                .build();

        HoroscopeGenerator horoscopeGenerator = AgenticServices.agentBuilder(HoroscopeGenerator.class)
                .chatModel(chatModel)
                .outputKey("horoscope")
                .build();

        PersonExtractor personExtractor = AgenticServices.agentBuilder(PersonExtractor.class)
                .chatModel(chatModel)
                .outputKey("person")
                .build();

        SignExtractor signExtractor = AgenticServices.agentBuilder(SignExtractor.class)
                .chatModel(chatModel)
                .outputKey("sign")
                .build();

        Writer writer = AgenticServices.agentBuilder(Writer.class)
                .chatModel(chatModel)
                .outputKey("writeup")
                .build();

        StoryFinder storyFinder = AgenticServices.agentBuilder(StoryFinder.class)
                .chatModel(chatModel)
                .tools(WebSearchTool.from(request -> WebSearchResults.from(
                        WebSearchInformationResult.from(1L),
                        List.of(WebSearchOrganicResult.from(
                                "Un Sagitario intentó predecir el futuro con una bola de cristal y acabó pidiendo pizza",
                                URI.create("https://example.com/funny-story"),
                                "Noticias ficticias del universo",
                                "Un astrólogo sagitariano aseguró haber visto el futuro en su bola de cristal, pero sólo vio su reflejo pidiendo una pizza de pepperoni a las 3am."
                        ))
                )))
                .outputKey("story")
                .build();

        SupervisorAgent supervisorAgent = AgenticServices.supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(horoscopeGenerator, personExtractor, signExtractor, writer, storyFinder)
                .outputKey("writeup")
                .build();

        ask(supervisorAgent, "Mi nombre es Alejandro y soy Sagitario. Dame algo divertido para leer.");

    }

    private static void ask(SupervisorAgent supervisorAgent, String request) {
        print("");
        print("Pregunta: " + request);
        print(Format.sep());
        print(supervisorAgent.invoke(request));
    }

    private static void print(String msg) {
        System.out.println(Format.markdown(msg));
    }

}

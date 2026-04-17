package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.jtpadilla.example.util.Format;
import io.github.jtpadilla.example.util.GoogleModels;
import io.helidon.config.Config;

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

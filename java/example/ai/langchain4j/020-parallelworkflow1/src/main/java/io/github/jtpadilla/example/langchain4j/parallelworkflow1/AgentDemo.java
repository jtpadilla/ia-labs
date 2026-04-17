package io.github.jtpadilla.example.langchain4j.parallelworkflow1;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.jtpadilla.example.util.Format;
import io.helidon.config.Config;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AgentDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public interface FoodExpert {

        @UserMessage("""
        Eres un excelente planificador de noches.
        Propón una lista de 3 comidas que se adapten al estado de ánimo indicado.
        El estado de ánimo es {{mood}}.
        Para cada comida, indica solo el nombre.
        Proporciona una lista con los 3 elementos y nada más.
        """)
        @Agent
        List<String> findMeal(@V("mood") String mood);
    }

    public interface MovieExpert {

        @UserMessage("""
        Eres un excelente planificador de noches.
        Propón una lista de 3 películas que se adapten al estado de ánimo indicado.
        El estado de ánimo es {{mood}}.
        Proporciona una lista con los 3 elementos y nada más.
        """)
        @Agent
        List<String> findMovie(@V("mood") String mood);
    }

    public record EveningPlan(String movie, String meal) {}

    public interface EveningPlannerAgent {
        @Agent
        List<EveningPlan> plan(@V("mood") String mood);
    }

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        FoodExpert foodExpert = AgenticServices
                .agentBuilder(FoodExpert.class)
                .chatModel(chatModel)
                .outputKey("meals")
                .build();

        MovieExpert movieExpert = AgenticServices
                .agentBuilder(MovieExpert.class)
                .chatModel(chatModel)
                .outputKey("movies")
                .build();

        Function<AgenticScope, Object> outputAdapterFunction = agenticScope -> {
            List<String> movies = agenticScope.readState("movies", List.of());
            List<String> meals = agenticScope.readState("meals", List.of());
            int size = Math.min(movies.size(), meals.size());
            return IntStream.range(0, size)
                    .mapToObj(i -> new EveningPlan(movies.get(i), meals.get(i)))
                    .toList();
        };

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
                EveningPlannerAgent eveningPlannerAgent = AgenticServices
                .parallelBuilder(EveningPlannerAgent.class)
                .subAgents(foodExpert, movieExpert)
                .executor(executorService)
                .outputKey("plans")
                .output(outputAdapterFunction)
                .build();

            List<EveningPlan> plans = eveningPlannerAgent.plan("romantic");

            String output = plans.stream()
                    .map(p -> "- **" + p.movie() + "** con *" + p.meal() + "*")
                    .collect(Collectors.joining("\n"));
            System.out.println(Format.markdown(output));

        }

    }

}
package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.GoalOrientedAgentImpl;
import io.github.jtpadilla.example.util.Format;
import io.github.jtpadilla.example.util.GoogleModels;
import io.helidon.config.Config;

import java.util.Map;

public class AgentDemo {

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("La clave de configuración 'gemini-api-key' es obligatoria"));

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(GoogleModels.geminiFlashLite())
                .logRequestsAndResponses(true)
                // thinking activado para que el modelo razone antes de responder
                .sendThinking(true)
                .returnThinking(true)
                .build();

        UntypedAgent horoscopeAgent = GoalOrientedAgentImpl.build(chatModel);

        // La clave "prompt" es el único input inicial; el planner deduce el resto de la cadena
        Map<String, Object> input = Map.of("prompt", "Me llamo Mario y mi signo zodiacal es piscis");
        String writeup = (String) horoscopeAgent.invoke(input);
        System.out.println(Format.markdown(writeup));

    }

}


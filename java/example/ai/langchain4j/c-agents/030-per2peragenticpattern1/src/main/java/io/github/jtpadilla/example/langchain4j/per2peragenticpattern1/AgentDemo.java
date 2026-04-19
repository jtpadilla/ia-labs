package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.ResearchAgent;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.ResearchAgentImpl;
import io.github.jtpadilla.example.util.Format;
import io.github.jtpadilla.example.util.GoogleModels;
import io.helidon.config.Config;

public class AgentDemo {

    private static final String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("La clave de configuración 'gemini-api-key' es obligatoria"));

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(GoogleModels.geminiFlashLite())
                .logRequestsAndResponses(true)
                .sendThinking(true)
                .returnThinking(true)
                .build();

        ResearchAgent researcher = ResearchAgentImpl.build(chatModel);
        String hypothesis = researcher.research("agujeros negros");
        System.out.println(Format.markdown(hypothesis));

    }

}

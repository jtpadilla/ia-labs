package io.github.jtpadilla.example.langchain4j.loopworkflow1;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.jtpadilla.example.format.Format;
import io.helidon.config.Config;

public class AgentDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public interface CreativeWriter {
        @UserMessage("""
            Eres un escritor creativo.
            Genera un borrador de una historia de no más de
            3 frases sobre el tema indicado.
            Devuelve solo la historia y nada más.
            El tema es {{topic}}.
            """)
        @Agent("Genera una historia basada en el tema indicado")
        String generateStory(@V("topic") String topic);
    }

    public interface StyleScorer {
        @UserMessage("""
            Eres un crítico literario.
            Asigna una puntuación entre 0.0 y 1.0 a la siguiente
            historia según cómo de bien se alinea con el estilo '{{style}}'.
            Devuelve solo la puntuación y nada más.

            La historia es: "{{story}}"
            """)
        @Agent("Puntúa una historia según cómo de bien se alinea con un estilo dado")
        double scoreStyle(@V("story") String story, @V("style") String style);
    }

    public interface StyleEditor {
        @UserMessage("""
            Eres un editor profesional.
            Analiza y reescribe la siguiente historia para que encaje mejor y sea más coherente con el estilo {{style}}.
            Devuelve solo la historia y nada más.
            La historia es "{{story}}".
            """)
        @Agent("Edita una historia para adaptarla mejor al estilo indicado")
        String editStory(@V("story") String story, @V("style") String style);
    }

    public interface StyledWriter {
        @Agent
        String writeStoryWithStyle(@V("topic") String topic, @V("style") String style);
    }

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        StyleScorer styleScorer = AgenticServices
                .agentBuilder(StyleScorer.class)
                .chatModel(chatModel)
                .outputKey("score")
                .build();

        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        UntypedAgent styleReviewLoop = AgenticServices
                .loopBuilder()
                .subAgents(styleScorer, styleEditor)
                .maxIterations(5)
                .exitCondition( agenticScope -> agenticScope.readState("score", 0.0) >= 0.8)
                .build();

        StyledWriter styledWriter = AgenticServices
                .sequenceBuilder(StyledWriter.class)
                .subAgents(creativeWriter, styleReviewLoop)
                .outputKey("story")
                .build();

        String story = styledWriter.writeStoryWithStyle("dragons and wizards", "comedy");
        System.out.println(Format.markdown(story));

    }

}
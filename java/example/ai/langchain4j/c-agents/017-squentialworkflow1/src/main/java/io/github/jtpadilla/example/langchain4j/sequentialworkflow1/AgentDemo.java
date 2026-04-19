package io.github.jtpadilla.example.langchain4j.sequentialworkflow1;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.jtpadilla.example.util.Format;
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

    public interface AudienceEditor {
        @UserMessage("""
            Eres un editor profesional.
            Analiza y reescribe la siguiente historia para adaptarla mejor
            al público objetivo de {{audience}}.
            Devuelve solo la historia y nada más.
            La historia es "{{story}}".
            """)
        @Agent("Edita una historia para adaptarla mejor al público indicado")
        String editStory(@V("story") String story, @V("audience") String audience);
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

    public interface NovelCreator {
        @Agent
        String createNovel(@V("topic") String topic, @V("audience") String audience, @V("style") String style);
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

        AudienceEditor audienceEditor = AgenticServices
                .agentBuilder(AudienceEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        NovelCreator novelCreator = AgenticServices
                .sequenceBuilder(NovelCreator.class)
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .outputKey("story")
                .build();

        String story = novelCreator.createNovel("dragons and wizards", "young adults", "fantasy");
        System.out.println(Format.markdown(story));

    }

}
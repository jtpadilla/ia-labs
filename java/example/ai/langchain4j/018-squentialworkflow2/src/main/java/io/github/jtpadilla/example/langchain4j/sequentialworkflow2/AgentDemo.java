package io.github.jtpadilla.example.langchain4j.sequentialworkflow2;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.util.Format;
import io.helidon.config.Config;

import java.util.Map;

public class AgentDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));


    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        UntypedAgent creativeWriter = AgenticServices.agentBuilder()
                .chatModel(chatModel)
                .description("Genera una historia basada en el tema indicado")
                .userMessage("""
                Eres un escritor creativo.
                Genera un borrador de una historia de no más de
                3 frases sobre el tema indicado.
                Devuelve solo la historia y nada más.
                El tema es {{topic}}.
                """)
                .inputKey(String.class, "topic")
                .returnType(String.class) // String is the default return type for untyped agents
                .outputKey("story")
                .build();

        UntypedAgent audienceEditor = AgenticServices.agentBuilder()
                .chatModel(chatModel)
                .description("Edita una historia para adaptarla mejor al público indicado")
                .userMessage("""
                Eres un editor profesional.
                Analiza y reescribe la siguiente historia para adaptarla mejor
                al público objetivo de {{audience}}.
                Devuelve solo la historia y nada más.
                La historia es "{{story}}".
                """)
                .inputKey(String.class, "audience")
                .inputKey(String.class, "story")
                .returnType(String.class) // String is the default return type for untyped agents
                .outputKey("story")
                .build();


        UntypedAgent styleEditor = AgenticServices.agentBuilder()
                .chatModel(chatModel)
                .description("Edita una historia para adaptarla mejor al estilo indicado")
                .userMessage("""
                Eres un editor profesional.
                Analiza y reescribe la siguiente historia para que encaje mejor y sea más coherente con el estilo {{style}}.
                Devuelve solo la historia y nada más.
                La historia es "{{story}}".
                """)
                .inputKey(String.class, "style")
                .inputKey(String.class, "story")
                .returnType(String.class) // String is the default return type for untyped agents
                .outputKey("story")
                .build();

        UntypedAgent novelCreator = AgenticServices
                .sequenceBuilder()
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .outputKey("story")
                .build();

        Map<String, Object> input = Map.of(
                "topic", "ciencia ficcion galactica",
                "style", "fantasía",
                "audience", "infantil"
        );

        String story = (String) novelCreator.invoke(input);
        System.out.println(Format.markdown(story));

    }

}
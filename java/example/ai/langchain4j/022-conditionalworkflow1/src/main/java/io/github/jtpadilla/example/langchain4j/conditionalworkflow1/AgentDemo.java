package io.github.jtpadilla.example.langchain4j.conditionalworkflow1;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
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

    public enum RequestCategory {
        LEGAL, MEDICAL, TECHNICAL, UNKNOWN
    }

    public interface CategoryRouter {
        @UserMessage("""
        Analiza la siguiente solicitud del usuario y categorízala como 'legal', 'medical' o 'technical'.
        Si la solicitud no pertenece a ninguna de esas categorías, categorízala como 'unknown'.
        Responde únicamente con una de esas palabras y nada más.
        La solicitud del usuario es: '{{request}}'.
        """)
        @Agent("Categoriza una solicitud de usuario")
        RequestCategory classify(@V("request") String request);
    }

    public interface MedicalExpert {
        @UserMessage("""
        Eres un experto médico.
        Analiza la siguiente solicitud del usuario desde un punto de vista médico y proporciona la mejor respuesta posible.
        La solicitud del usuario es {{request}}.
        """)
        @Agent("Un experto médico")
        String medical(@V("request") String request);
    }

    public interface LegalExpert {
        @UserMessage("""
        Eres un experto jurídico.
        Analiza la siguiente solicitud del usuario desde un punto de vista legal y proporciona la mejor respuesta posible.
        La solicitud del usuario es {{request}}.
        """)
        @Agent("Un experto jurídico")
        String legal(@V("request") String request);
    }

    public interface TechnicalExpert {
        @UserMessage("""
        Eres un experto técnico en tecnología e ingeniería.
        Analiza la siguiente solicitud del usuario desde un punto de vista técnico y proporciona la mejor respuesta posible.
        La solicitud del usuario es {{request}}.
        """)
        @Agent("Un experto técnico")
        String technical(@V("request") String request);
    }

    public interface ExpertRouterAgent {
        @UserMessage("{{request}}")
        @Agent("Enruta la solicitud al experto adecuado y devuelve su respuesta")
        String ask(@V("request") String request);
    }

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        CategoryRouter routerAgent = AgenticServices
                .agentBuilder(CategoryRouter.class)
                .chatModel(chatModel)
                .outputKey("category")
                .build();

        MedicalExpert medicalExpert = AgenticServices
                .agentBuilder(MedicalExpert.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();

        LegalExpert legalExpert = AgenticServices
                .agentBuilder(LegalExpert.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();

        TechnicalExpert technicalExpert = AgenticServices
                .agentBuilder(TechnicalExpert.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();

        UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, medicalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL, legalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL, technicalExpert)
                .build();

        ExpertRouterAgent expertRouterAgent = AgenticServices
                .sequenceBuilder(ExpertRouterAgent.class)
                .subAgents(routerAgent, expertsAgent)
                .outputKey("response")
                .build();

        String response = expertRouterAgent.ask("Me he roto el coche, ¿qué debo hacer?");

        System.out.println(Format.markdown(response));

    }

}

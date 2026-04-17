package io.github.jtpadilla.example.langchain4j.stronglytyped1;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.UserMessage;
import io.github.jtpadilla.example.format.Format;
import io.helidon.config.Config;

import java.nio.file.Path;

public class AgentDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public enum RequestCategory {
        LEGAL, MEDICAL, TECHNICAL, UNKNOWN
    }

    public static class UserRequest implements TypedKey<String> { }

    public static class ExpertResponse implements TypedKey<String> { }

    public static class Category implements TypedKey<RequestCategory> {
        @Override
        public RequestCategory defaultValue() {
            return RequestCategory.UNKNOWN;
        }
    }

    public interface CategoryRouter {
        @UserMessage("""
        Analiza la siguiente solicitud del usuario y categorízala como 'legal', 'medical' o 'technical'.
        Si la solicitud no pertenece a ninguna de esas categorías, categorízala como 'unknown'.
        Responde únicamente con una de esas palabras y nada más.
        La solicitud del usuario es: '{{request}}'.
        """)
        @Agent("Categoriza una solicitud de usuario")
        RequestCategory classify(@K(UserRequest.class) String request);
    }

    public interface MedicalExpert {
        @UserMessage("""
        Eres un experto médico.
        Analiza la siguiente solicitud del usuario desde un punto de vista médico y proporciona la mejor respuesta posible.
        La solicitud del usuario es {{request}}.
        """)
        @Agent("Un experto médico")
        String medical(@K(UserRequest.class) String request);
    }

    public interface LegalExpert {
        @UserMessage("""
        Eres un experto jurídico.
        Analiza la siguiente solicitud del usuario desde un punto de vista legal y proporciona la mejor respuesta posible.
        La solicitud del usuario es {{request}}.
        """)
        @Agent("Un experto jurídico")
        String legal(@K(UserRequest.class) String request);
    }

    public interface TechnicalExpert {
        @UserMessage("""
        Eres un experto técnico en tecnología e ingeniería.
        Analiza la siguiente solicitud del usuario desde un punto de vista técnico y proporciona la mejor respuesta posible.
        La solicitud del usuario es {{request}}.
        """)
        @Agent("Un experto técnico")
        String technical(@K(UserRequest.class) String request);
    }

    public interface ExpertRouterAgent {
        @UserMessage("{{request}}")
        @Agent("Enruta la solicitud al experto adecuado y devuelve su respuesta")
        String ask(@K(UserRequest.class) String request);
    }

    public static void main(String[] args) {

        AgentMonitor monitor = new AgentMonitor();

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        CategoryRouter routerAgent = AgenticServices
                .agentBuilder(CategoryRouter.class)
                .chatModel(chatModel)
                .outputKey(Category.class)
                .build();

        MedicalExpert medicalExpert = AgenticServices
                .agentBuilder(MedicalExpert.class)
                .chatModel(chatModel)
                .outputKey(ExpertResponse.class)
                .build();

        LegalExpert legalExpert = AgenticServices
                .agentBuilder(LegalExpert.class)
                .chatModel(chatModel)
                .outputKey(ExpertResponse.class)
                .build();

        TechnicalExpert technicalExpert = AgenticServices
                .agentBuilder(TechnicalExpert.class)
                .chatModel(chatModel)
                .outputKey(ExpertResponse.class)
                .build();

        UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
                .subAgents(agenticScope -> agenticScope.readState(Category.class) == RequestCategory.MEDICAL, medicalExpert)
                .subAgents(agenticScope -> agenticScope.readState(Category.class) == RequestCategory.LEGAL, legalExpert)
                .subAgents(agenticScope -> agenticScope.readState(Category.class) == RequestCategory.TECHNICAL, technicalExpert)
                .build();

        ExpertRouterAgent expertRouterAgent = AgenticServices
                .sequenceBuilder(ExpertRouterAgent.class)
                .subAgents(routerAgent, expertsAgent)
                .outputKey(ExpertResponse.class)
                .listener(monitor)
                .build();

        String response = expertRouterAgent.ask("Me he roto el coche, ¿qué debo hacer?");

        System.out.println(Format.markdown(response));
        HtmlReportGenerator.generateReport(monitor, Path.of(System.getProperty("user.home"), "028-stronglytyped1.html"));

    }

}

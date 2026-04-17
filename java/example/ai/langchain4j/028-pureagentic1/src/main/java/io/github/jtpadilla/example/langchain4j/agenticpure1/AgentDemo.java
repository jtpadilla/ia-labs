package io.github.jtpadilla.example.langchain4j.agenticpure1;

import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.util.Format;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.SupervisorAgentImpl;
import io.github.jtpadilla.example.langchain4j.agenticpure1.tool.BankTool;
import io.helidon.config.Config;

public class AgentDemo {

    record Models(String supervisor, String agent) {}

    // Supervisor con thinking + sub-agentes con el mismo modelo (sin thinking en sub-agentes)
    static Models geminiFlashLite() {
        return new Models("gemini-3.1-flash-lite-preview", "gemini-3.1-flash-lite-preview");
    }

    // Supervisor con thinking (Gemini) + sub-agentes sin thinking (Gemma 26b)
    static Models geminiSupervisorGemma26bAgents() {
        return new Models("gemini-3.1-flash-lite-preview", "gemma-4-26b-a4b-it");
    }

    // Supervisor y sub-agentes con Gemma 26b (~1 min 35 s)
    static Models gemma26b() {
        return new Models("gemma-4-26b-a4b-it", "gemma-4-26b-a4b-it");
    }

    // Supervisor y sub-agentes con Gemma 31b (~1 min 32 s)
    static Models gemma31b() {
        return new Models("gemma-4-31b-it", "gemma-4-31b-it");
    }


    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        Models models = geminiSupervisorGemma26bAgents();

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(models.agent())
                .logRequestsAndResponses(true)
                .build();

        ChatModel chatModelThinking = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(models.supervisor())
                .logRequestsAndResponses(true)
                .sendThinking(true)
                .returnThinking(true)
                .build();

        BankTool bankTool = new BankTool();
        bankTool.createAccount("Mario", 1000.0);
        bankTool.createAccount("Georgios", 1000.0);

        SupervisorAgent supervisorAgent = SupervisorAgentImpl.build(chatModelThinking, chatModel, bankTool);

        ask(supervisorAgent, "Muéstrame una tabla con el estado de las cuentas.");
        ask(supervisorAgent, "Trasfiere 100$ desde la cuenta de 'Mario' a la cuenta de 'Georgios'");
        ask(supervisorAgent, "Muéstrame como están ahora las cuentas.");

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

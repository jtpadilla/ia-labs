package io.github.jtpadilla.example.langchain4j.agenticpure1;

import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.format.Format;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.SupervisorAgentImpl;
import io.github.jtpadilla.example.langchain4j.agenticpure1.tool.BankTool;
import io.helidon.config.Config;

public class AgentDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .sendThinking(true)
                .returnThinking(true)
                .build();

        BankTool bankTool = new BankTool();
        bankTool.createAccount("Mario", 1000.0);
        bankTool.createAccount("Georgios", 1000.0);

        SupervisorAgent supervisorAgent = SupervisorAgentImpl.build(chatModel, bankTool);

        ask(supervisorAgent, "Muéstrame una tabla con el estado de las cuentas.");
        ask(supervisorAgent, "Trasfiere 100 EUR desde la cuenta de 'Mario' a la cuenta de 'Georgios'");
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

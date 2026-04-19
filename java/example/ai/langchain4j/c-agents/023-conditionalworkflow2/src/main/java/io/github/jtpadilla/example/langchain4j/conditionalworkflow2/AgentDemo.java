package io.github.jtpadilla.example.langchain4j.conditionalworkflow2;

import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.ExpertRouter;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.ExpertRouterImpl;
import io.github.jtpadilla.example.util.Format;
import io.helidon.config.Config;

import java.nio.file.Path;

public class AgentDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    private static final String API_KEY = Config.global()
            .get("gemini-api-key")
            .asString()
            .orElseThrow(() -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        AgentMonitor monitor = new AgentMonitor();

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        ExpertRouter agent = ExpertRouterImpl.build(chatModel, monitor);

        // Caso técnico → ingeniería mecánica (automoción)
        ask(agent, "Me he roto el coche, ¿qué debo hacer?");

        // Caso técnico → ingeniería de software
        ask(agent, "¿Qué patrón de diseño debo usar para desacoplar la lógica de negocio de la capa de persistencia en Java?");

        // Caso médico
        ask(agent, "Llevo tres días con fiebre alta y dolor de cabeza, ¿qué puede ser?");

        // Caso jurídico
        ask(agent, "Mi arrendador no me devuelve la fianza sin motivo justificado, ¿qué puedo hacer?");

        // Caso técnico → ingeniería civil
        ask(agent, "¿Cuánto armado necesita una losa de cimentación de 20 cm para una vivienda unifamiliar?");

        // Caso técnico → ingeniería hardware
        ask(agent, "¿Cómo conecto un sensor de temperatura DS18B20 a un ESP32 con resistencia pull-up?");

        HtmlReportGenerator.generateReport(monitor, Path.of(System.getProperty("user.home"), "023-conditionalworkflow2.html"));

    }

    private static void ask(ExpertRouter agent, String request) {
        print("");
        print("Pregunta: " + request);
        print(Format.sep());
        print(agent.ask(request));
    }

    private static void print(String msg) {
        System.out.println(Format.markdown(msg));
    }

}

package io.github.jtpadilla.example.langchain4j.conditionalworkflow2;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.format.Format;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level0.expertrouteragent.ExpertRouterAgent;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level0.expertrouteragent.ExpertRouterAgentImpl;
import io.helidon.config.Config;

public class AgentDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    private static final String API_KEY = Config.global()
            .get("gemini-api-key")
            .asString()
            .orElseThrow(() -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        ExpertRouterAgent agent = ExpertRouterAgentImpl.build(chatModel);

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

    }

    private static void ask(ExpertRouterAgent agent, String request) {
        System.out.println("=".repeat(80));
        System.out.println("Pregunta: " + request);
        System.out.println("-".repeat(80));
        String response = agent.ask(request);
        System.out.println(Format.markdown(response));
    }

}

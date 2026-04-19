package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.softwareengineer;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Factoría para construir el agente {@link SoftwareEngineer}.
 *
 * <p>Registra la salida del agente bajo la clave {@code "response"} del scope.
 */
public class SoftwareEngineerImpl {

    static public SoftwareEngineer build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(SoftwareEngineer.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();
    }

}

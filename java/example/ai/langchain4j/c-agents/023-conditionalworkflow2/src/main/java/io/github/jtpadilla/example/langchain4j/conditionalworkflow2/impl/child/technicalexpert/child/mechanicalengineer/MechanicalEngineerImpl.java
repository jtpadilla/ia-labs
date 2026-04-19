package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.mechanicalengineer;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Factoría para construir el agente {@link MechanicalEngineer}.
 *
 * <p>Registra la salida del agente bajo la clave {@code "response"} del scope.
 */
public class MechanicalEngineerImpl {

    static public MechanicalEngineer build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(MechanicalEngineer.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();
    }

}

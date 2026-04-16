package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Factoría para construir el agente {@link EngineeringSelector}.
 *
 * <p>Registra la salida del agente bajo la clave {@code "engineering_category"} del scope.
 */
public class EngineeringSelectorImpl {

    static public EngineeringSelector build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(EngineeringSelector.class)
                .chatModel(chatModel)
                .outputKey("engineering_category")
                .build();
    }

}

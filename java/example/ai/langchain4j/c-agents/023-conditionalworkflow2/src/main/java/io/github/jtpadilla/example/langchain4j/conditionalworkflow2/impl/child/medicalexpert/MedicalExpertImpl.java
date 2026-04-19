package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.medicalexpert;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Factoría para construir el agente {@link MedicalExpert}.
 *
 * <p>Registra la salida del agente bajo la clave {@code "response"} del scope.
 */
public class MedicalExpertImpl {

    static public MedicalExpert build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(MedicalExpert.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();
    }

}

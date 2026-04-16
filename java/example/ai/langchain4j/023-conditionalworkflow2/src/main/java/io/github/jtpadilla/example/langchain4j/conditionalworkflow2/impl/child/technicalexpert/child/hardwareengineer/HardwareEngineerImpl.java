package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.hardwareengineer;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

public class HardwareEngineerImpl {

    static public HardwareEngineer build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(HardwareEngineer.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();
    }

}

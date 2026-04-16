package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level2.mechanicalengineer;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

public class MechanicalEngineerImpl {

    static public MechanicalEngineer build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(MechanicalEngineer.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();
    }

}

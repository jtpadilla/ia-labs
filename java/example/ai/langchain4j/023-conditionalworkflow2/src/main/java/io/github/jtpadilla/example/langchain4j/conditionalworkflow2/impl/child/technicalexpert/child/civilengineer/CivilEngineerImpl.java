package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.civilengineer;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

public class CivilEngineerImpl {

    static public CivilEngineer build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(CivilEngineer.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();
    }

}

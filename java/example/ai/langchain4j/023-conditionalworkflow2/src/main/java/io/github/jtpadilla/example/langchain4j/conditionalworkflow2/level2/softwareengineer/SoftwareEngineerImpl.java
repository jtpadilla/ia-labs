package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level2.softwareengineer;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

public class SoftwareEngineerImpl {

    static public SoftwareEngineer build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(SoftwareEngineer.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();
    }

}

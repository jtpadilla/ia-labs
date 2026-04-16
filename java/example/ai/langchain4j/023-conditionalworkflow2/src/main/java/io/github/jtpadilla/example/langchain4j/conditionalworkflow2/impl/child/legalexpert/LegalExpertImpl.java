package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.legalexpert;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

public class LegalExpertImpl {

    static public LegalExpert build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(LegalExpert.class)
                .chatModel(chatModel)
                .outputKey("response")
                .build();
    }

}

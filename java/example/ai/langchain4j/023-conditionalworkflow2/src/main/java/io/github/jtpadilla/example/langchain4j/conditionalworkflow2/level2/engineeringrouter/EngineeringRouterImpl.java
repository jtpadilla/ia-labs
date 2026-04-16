package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level2.engineeringrouter;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

public class EngineeringRouterImpl {

    static public EngineeringRouter build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(EngineeringRouter.class)
                .chatModel(chatModel)
                .outputKey("engineering_category")
                .build();
    }

}

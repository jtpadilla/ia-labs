package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.signextractor;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

public class SignExtractorImpl {

    static public SignExtractor build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(SignExtractor.class)
                .chatModel(chatModel)
                .outputKey("sign")
                .build();
    }

}

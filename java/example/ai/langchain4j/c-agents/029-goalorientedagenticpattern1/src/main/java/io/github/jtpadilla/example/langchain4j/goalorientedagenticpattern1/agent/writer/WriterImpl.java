package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.writer;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

public class WriterImpl {

    static public Writer build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(Writer.class)
                .chatModel(chatModel)
                .outputKey("writeup")
                .build();
    }

}

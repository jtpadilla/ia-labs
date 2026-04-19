package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.personextractor;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

public class PersonExtractorImpl {

    static public PersonExtractor build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(PersonExtractor.class)
                .chatModel(chatModel)
                .outputKey("person")
                .build();
    }

}

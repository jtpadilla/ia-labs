package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.horoscopegenerator;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

public class HoroscopeGeneratorImpl {

    static public HoroscopeGenerator build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(HoroscopeGenerator.class)
                .chatModel(chatModel)
                .outputKey("horoscope")
                .build();
    }

}

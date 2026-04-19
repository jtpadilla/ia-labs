package io.github.jtpadilla.example.langchain4j.agenticpure1.agent.exchangeagent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.agenticpure1.tool.ExchangeTool;

public class ExchangeAgentImpl {

    static public ExchangeAgent build(ChatModel chatModel, ExchangeTool exchangeTool) {
        return AgenticServices
                .agentBuilder(ExchangeAgent.class)
                .chatModel(chatModel)
                .tools(exchangeTool)
                .build();
    }

}

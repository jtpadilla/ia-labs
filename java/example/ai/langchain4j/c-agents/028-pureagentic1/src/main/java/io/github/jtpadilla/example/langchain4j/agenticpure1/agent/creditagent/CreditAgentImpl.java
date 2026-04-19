package io.github.jtpadilla.example.langchain4j.agenticpure1.agent.creditagent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.agenticpure1.tool.BankTool;

public class CreditAgentImpl {

    static public CreditAgent build(ChatModel chatModel, BankTool bankTool) {
        return AgenticServices
                .agentBuilder(CreditAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();
    }

}

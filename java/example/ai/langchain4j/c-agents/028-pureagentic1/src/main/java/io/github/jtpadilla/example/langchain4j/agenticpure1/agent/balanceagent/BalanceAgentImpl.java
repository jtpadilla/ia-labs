package io.github.jtpadilla.example.langchain4j.agenticpure1.agent.balanceagent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.agenticpure1.tool.BankTool;

public class BalanceAgentImpl {

    static public BalanceAgent build(ChatModel chatModel, BankTool bankTool) {
        return AgenticServices
                .agentBuilder(BalanceAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();
    }

}

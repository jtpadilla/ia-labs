package io.github.jtpadilla.example.langchain4j.agenticpure1.agent.withdrawagent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.agenticpure1.tool.BankTool;

public class WithdrawAgentImpl {

    static public WithdrawAgent build(ChatModel chatModel, BankTool bankTool) {
        return AgenticServices
                .agentBuilder(WithdrawAgent.class)
                .chatModel(chatModel)
                .tools(bankTool)
                .build();
    }

}

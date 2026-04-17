package io.github.jtpadilla.example.langchain4j.agenticpure1.agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.creditagent.CreditAgent;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.creditagent.CreditAgentImpl;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.exchangeagent.ExchangeAgent;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.exchangeagent.ExchangeAgentImpl;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.withdrawagent.WithdrawAgent;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.withdrawagent.WithdrawAgentImpl;
import io.github.jtpadilla.example.langchain4j.agenticpure1.tool.BankTool;
import io.github.jtpadilla.example.langchain4j.agenticpure1.tool.ExchangeTool;

public class SupervisorAgentImpl {

    static public SupervisorAgent build(ChatModel chatModel, BankTool bankTool) {

        WithdrawAgent withdrawAgent = WithdrawAgentImpl.build(chatModel, bankTool);
        CreditAgent creditAgent = CreditAgentImpl.build(chatModel, bankTool);
        ExchangeAgent exchangeAgent = ExchangeAgentImpl.build(chatModel, new ExchangeTool());

        return AgenticServices
                .supervisorBuilder()
                .chatModel(chatModel)
                //.chatModel(PLANNER_MODEL) // En el ejemplo original habla de este modelo pero no se en que se diferencia.
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();
    }

}

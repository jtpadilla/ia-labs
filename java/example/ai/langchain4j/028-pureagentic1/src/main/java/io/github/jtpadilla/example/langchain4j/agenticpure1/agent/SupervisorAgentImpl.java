package io.github.jtpadilla.example.langchain4j.agenticpure1.agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.balanceagent.BalanceAgent;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.balanceagent.BalanceAgentImpl;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.creditagent.CreditAgent;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.creditagent.CreditAgentImpl;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.exchangeagent.ExchangeAgent;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.exchangeagent.ExchangeAgentImpl;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.withdrawagent.WithdrawAgent;
import io.github.jtpadilla.example.langchain4j.agenticpure1.agent.withdrawagent.WithdrawAgentImpl;
import io.github.jtpadilla.example.langchain4j.agenticpure1.tool.BankTool;
import io.github.jtpadilla.example.langchain4j.agenticpure1.tool.ExchangeTool;

public class SupervisorAgentImpl {

    static public SupervisorAgent build(ChatModel chatModelThinking, ChatModel chatModel, BankTool bankTool) {

        BalanceAgent balanceAgent = BalanceAgentImpl.build(chatModel, bankTool);
        WithdrawAgent withdrawAgent = WithdrawAgentImpl.build(chatModel, bankTool);
        CreditAgent creditAgent = CreditAgentImpl.build(chatModel, bankTool);
        ExchangeAgent exchangeAgent = ExchangeAgentImpl.build(chatModel, new ExchangeTool());

        return AgenticServices
                .supervisorBuilder()
                .chatModel(chatModelThinking)
                //.chatModel(PLANNER_MODEL) // En el ejemplo original habla de este modelo pero no se en que se diferencia.
                .supervisorContext("""
                        Todas las operaciones bancarias se realizan exclusivamente en euros (EUR).
                        Si el usuario solicita una operación con un importe en otra divisa, utiliza primero
                        el agente de cambio para convertir dicho importe a EUR y usa el resultado para
                        la operación bancaria correspondiente.
                        Responde siempre al usuario en español.
                        Formatea siempre tus respuestas en Markdown rico: negritas, cursivas,
                        listas y encabezados cuando aporten claridad pero nunca tablas. Nunca respondas con texto plano.
                        """)
                .subAgents(balanceAgent, withdrawAgent, creditAgent, exchangeAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();
    }

}

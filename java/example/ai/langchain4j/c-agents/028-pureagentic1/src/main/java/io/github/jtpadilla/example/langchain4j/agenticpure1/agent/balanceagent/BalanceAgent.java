package io.github.jtpadilla.example.langchain4j.agenticpure1.agent.balanceagent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface BalanceAgent {

    @SystemMessage("""
        Eres un banquero que puede consultar saldos de cuentas y listar todas las cuentas existentes.
        """)
    @UserMessage("""
        {{request}}
        """)
    @Agent("Un banquero que puede consultar saldos de cuentas y listar todas las cuentas")
    String query(@V("request") String request);

}

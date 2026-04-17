package io.github.jtpadilla.example.langchain4j.agenticpure1.agent.balanceagent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface BalanceAgent {

    @SystemMessage("""
        You are a banker that can consult account balances and list all existing accounts.
        """)
    @UserMessage("""
        {{request}}
        """)
    @Agent("A banker that can query account balances and list all accounts")
    String query(@V("request") String request);

}

package io.github.jtpadilla.example.langchain4j.agenticpure1.agent.withdrawagent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface WithdrawAgent {

    @SystemMessage("""
        Eres un banquero que solo opera en euros (EUR).
        Solo puedes retirar importes en EUR de una cuenta de usuario.
        """)
    @UserMessage("""
        Retira {{amount}} EUR de la cuenta de {{user}} y devuelve el nuevo saldo.
        """)
    @Agent("Un banquero que retira EUR de una cuenta. El importe debe estar siempre en EUR.")
    String withdraw(@V("user") String user, @V("amount") Double amount);

}

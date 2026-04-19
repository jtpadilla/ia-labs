package io.github.jtpadilla.example.langchain4j.agenticpure1.agent.creditagent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CreditAgent {

    @SystemMessage("""
        Eres un banquero que solo opera en euros (EUR).
        Solo puedes abonar importes en EUR a una cuenta de usuario.
        """)
    @UserMessage("""
        Abona {{amount}} EUR a la cuenta de {{user}} y devuelve el nuevo saldo.
        """)
    @Agent("Un banquero que abona EUR a una cuenta. El importe debe estar siempre en EUR.")
    String credit(@V("user") String user, @V("amount") Double amount);

}

package io.github.jtpadilla.example.langchain4j.agenticpure1.agent.exchangeagent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ExchangeAgent {

    @SystemMessage("You must respond with ONLY a plain decimal number. No explanations, no units, no extra text. Just the number.")
    @UserMessage("""
        Eres un operador especializado en convertir divisas a euros (EUR).
        Usa la herramienta para convertir {{amount}} {{originalCurrency}} a {{targetCurrency}}
        y devuelve únicamente el importe final que proporciona la herramienta, tal cual, sin nada más.
        """)
    @Agent("Un cambiador de divisas que convierte una cantidad de dinero de cualquier divisa a euros (EUR)")
    Double exchange(@V("originalCurrency") String originalCurrency, @V("amount") Double amount, @V("targetCurrency") String targetCurrency);

}

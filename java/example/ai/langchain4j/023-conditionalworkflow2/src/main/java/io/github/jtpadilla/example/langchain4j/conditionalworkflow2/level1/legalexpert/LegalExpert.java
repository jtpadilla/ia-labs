package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level1.legalexpert;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LegalExpert {

    @UserMessage("""
    Eres un experto jurídico.
    Analiza la siguiente solicitud del usuario desde un punto de vista legal y proporciona la mejor respuesta posible.
    La solicitud del usuario es {{request}}.
    """)
    @Agent("Un experto jurídico")
    String legal(@V("request") String request);

}

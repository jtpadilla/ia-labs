package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.legalexpert;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Experto jurídico de nivel 1.
 *
 * <p>Se activa cuando el dispatcher condicional detecta {@code category == LEGAL}.
 * Analiza la solicitud desde un punto de vista legal y escribe su respuesta
 * bajo la clave {@code "response"} del scope.
 */
public interface LegalExpert {

    @UserMessage("""
    Eres un experto jurídico.
    Analiza la siguiente solicitud del usuario desde un punto de vista legal y proporciona la mejor respuesta posible.
    La solicitud del usuario es {{request}}.
    """)
    @Agent("Un experto jurídico")
    String legal(@V("request") String request);

}

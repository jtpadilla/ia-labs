package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.medicalexpert;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Experto médico de nivel 1.
 *
 * <p>Se activa cuando el dispatcher condicional detecta {@code category == MEDICAL}.
 * Analiza la solicitud desde un punto de vista médico y escribe su respuesta
 * bajo la clave {@code "response"} del scope.
 */
public interface MedicalExpert {

    @UserMessage("""
    Eres un experto médico.
    Analiza la siguiente solicitud del usuario desde un punto de vista médico y proporciona la mejor respuesta posible.
    La solicitud del usuario es {{request}}.
    """)
    @Agent("Un experto médico")
    String medical(@V("request") String request);

}

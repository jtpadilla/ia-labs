package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agente clasificador de primer nivel.
 *
 * <p>Analiza el texto libre del usuario y lo clasifica en una de las categorías
 * definidas en {@link ExpertSelectorResult}. El resultado se escribe en el estado
 * del scope bajo la clave {@code "category"}.
 */
public interface ExpertSelector {

    @UserMessage("""
    Analiza la siguiente solicitud del usuario y categorízala como 'legal', 'medical' o 'technical'.
    Si la solicitud no pertenece a ninguna de esas categorías, categorízala como 'unknown'.
    Responde únicamente con una de esas palabras y nada más.
    La solicitud del usuario es: '{{request}}'.
    """)
    @Agent("Categoriza una solicitud de usuario en legal, medical, technical o unknown")
    ExpertSelectorResult classify(@V("request") String request);

}

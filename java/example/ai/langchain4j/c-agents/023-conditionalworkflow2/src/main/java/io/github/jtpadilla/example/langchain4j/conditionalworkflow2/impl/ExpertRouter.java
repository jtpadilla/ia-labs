package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agente raíz del flujo de enrutamiento experto.
 *
 * <p>Recibe la pregunta del usuario y la propaga a través de la secuencia:
 * {@link ExpertSelectorImpl} → dispatcher condicional → experto especializado.
 * El resultado final se almacena bajo la clave {@code "response"}.
 */
public interface ExpertRouter {

    @UserMessage("{{request}}")
    @Agent("Enruta la solicitud al experto adecuado y devuelve su respuesta")
    String ask(@V("request") String request);

}

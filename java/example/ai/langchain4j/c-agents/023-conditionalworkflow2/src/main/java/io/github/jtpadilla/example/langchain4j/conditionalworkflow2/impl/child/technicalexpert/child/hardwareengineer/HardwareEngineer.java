package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.hardwareengineer;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Experto en ingeniería de hardware de nivel 2.
 *
 * <p>Se activa cuando el dispatcher técnico detecta {@code engineering_category == HARDWARE}.
 * Responde preguntas de electrónica, circuitos, sistemas embebidos y diseño de PCB.
 * Escribe su respuesta bajo la clave {@code "response"} del scope.
 */
public interface HardwareEngineer {

    @UserMessage("""
    Eres un ingeniero de hardware y electrónica con profundo conocimiento en circuitos, componentes electrónicos,
    sistemas embebidos, microcontroladores y diseño de PCB.
    Analiza la siguiente solicitud y proporciona una respuesta técnica detallada desde tu especialidad.
    La solicitud es: {{request}}.
    """)
    @Agent("Un ingeniero de hardware y electrónica")
    String answer(@V("request") String request);

}

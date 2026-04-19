package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.softwareengineer;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Experto en ingeniería de software de nivel 2.
 *
 * <p>Se activa cuando el dispatcher técnico detecta {@code engineering_category == SOFTWARE}.
 * Responde preguntas de programación, arquitectura, redes e inteligencia artificial.
 * Escribe su respuesta bajo la clave {@code "response"} del scope.
 */
public interface SoftwareEngineer {

    @UserMessage("""
    Eres un ingeniero de software senior con amplia experiencia en programación, arquitectura de sistemas,
    redes, inteligencia artificial y desarrollo de aplicaciones.
    Analiza la siguiente solicitud y proporciona una respuesta técnica detallada desde tu especialidad.
    La solicitud es: {{request}}.
    """)
    @Agent("Un ingeniero de software senior")
    String answer(@V("request") String request);

}

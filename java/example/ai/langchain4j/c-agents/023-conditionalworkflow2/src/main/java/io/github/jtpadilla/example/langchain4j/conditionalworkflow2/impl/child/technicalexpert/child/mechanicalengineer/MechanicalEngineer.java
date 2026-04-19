package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.mechanicalengineer;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Experto en ingeniería mecánica de nivel 2.
 *
 * <p>Se activa cuando el dispatcher técnico detecta {@code engineering_category == MECHANICAL}.
 * Responde preguntas de maquinaria, automoción, termodinámica y mantenimiento industrial.
 * Escribe su respuesta bajo la clave {@code "response"} del scope.
 */
public interface MechanicalEngineer {

    @UserMessage("""
    Eres un ingeniero mecánico con amplia experiencia en maquinaria, automoción, termodinámica,
    fluidos, fabricación y mantenimiento industrial.
    Analiza la siguiente solicitud y proporciona una respuesta técnica detallada desde tu especialidad.
    La solicitud es: {{request}}.
    """)
    @Agent("Un ingeniero mecánico experto en maquinaria y automoción")
    String answer(@V("request") String request);

}

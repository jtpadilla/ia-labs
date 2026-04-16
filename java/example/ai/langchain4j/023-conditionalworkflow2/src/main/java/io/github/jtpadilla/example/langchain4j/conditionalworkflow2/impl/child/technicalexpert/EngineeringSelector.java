package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agente clasificador de segundo nivel para solicitudes técnicas.
 *
 * <p>Identifica la disciplina de ingeniería más adecuada ({@link EngineeringSelectorResult})
 * y escribe el resultado bajo la clave {@code "engineering_category"} del scope,
 * que es leída por el dispatcher condicional de nivel 2 en {@link EngineeringRouterImpl}.
 */
public interface EngineeringSelector {

    @UserMessage("""
    Analiza la siguiente solicitud técnica e identifica la disciplina de ingeniería más adecuada para responderla.
    Responde únicamente con una de estas palabras: 'software', 'hardware', 'civil' o 'mechanical'.
    - Usa 'software' para cuestiones de programación, sistemas, redes, inteligencia artificial o software en general.
    - Usa 'hardware' para cuestiones de electrónica, circuitos, componentes físicos o sistemas embebidos.
    - Usa 'civil' para cuestiones de construcción, estructuras, infraestructuras o urbanismo.
    - Usa 'mechanical' para cuestiones de maquinaria, automoción, termodinámica o fabricación.
    - Si no encaja en ninguna categoría, responde 'unknown'.
    La solicitud técnica es: '{{request}}'.
    """)
    @Agent("Clasifica una solicitud técnica en la disciplina de ingeniería correspondiente")
    EngineeringSelectorResult classify(@V("request") String request);

}

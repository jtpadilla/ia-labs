package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface EngineeringRouter {

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
    EngineeringCategory classify(@V("request") String request);

}

package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level0.categoryrouter;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CategoryRouter {

    @UserMessage("""
    Analiza la siguiente solicitud del usuario y categorízala como 'legal', 'medical' o 'technical'.
    Si la solicitud no pertenece a ninguna de esas categorías, categorízala como 'unknown'.
    Responde únicamente con una de esas palabras y nada más.
    La solicitud del usuario es: '{{request}}'.
    """)
    @Agent("Categoriza una solicitud de usuario en legal, medical, technical o unknown")
    RequestCategory classify(@V("request") String request);

}

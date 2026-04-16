package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.civilengineer;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CivilEngineer {

    @UserMessage("""
    Eres un ingeniero civil con amplia experiencia en construcción, estructuras, cimentaciones,
    infraestructuras, urbanismo y materiales de construcción.
    Analiza la siguiente solicitud y proporciona una respuesta técnica detallada desde tu especialidad.
    La solicitud es: {{request}}.
    """)
    @Agent("Un ingeniero civil experto en construcción y estructuras")
    String answer(@V("request") String request);

}

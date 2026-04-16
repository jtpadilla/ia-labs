package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level2.softwareengineer;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

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

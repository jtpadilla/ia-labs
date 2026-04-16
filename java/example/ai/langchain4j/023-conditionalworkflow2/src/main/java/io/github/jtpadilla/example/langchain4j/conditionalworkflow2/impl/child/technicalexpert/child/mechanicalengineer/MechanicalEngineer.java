package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.mechanicalengineer;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

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

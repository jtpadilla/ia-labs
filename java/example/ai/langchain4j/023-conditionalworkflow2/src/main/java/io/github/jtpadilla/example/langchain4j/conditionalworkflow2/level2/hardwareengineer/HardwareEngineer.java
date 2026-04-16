package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level2.hardwareengineer;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

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

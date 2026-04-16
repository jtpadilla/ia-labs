package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level1.medicalexpert;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface MedicalExpert {

    @UserMessage("""
    Eres un experto médico.
    Analiza la siguiente solicitud del usuario desde un punto de vista médico y proporciona la mejor respuesta posible.
    La solicitud del usuario es {{request}}.
    """)
    @Agent("Un experto médico")
    String medical(@V("request") String request);

}

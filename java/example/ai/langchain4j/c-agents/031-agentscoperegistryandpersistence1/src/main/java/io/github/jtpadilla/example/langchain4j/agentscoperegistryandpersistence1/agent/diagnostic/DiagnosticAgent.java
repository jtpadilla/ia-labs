package io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent.diagnostic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DiagnosticAgent {

    @UserMessage("""
        Eres un experto en diagnóstico de problemas técnicos de software.
        Analiza el siguiente problema e identifica el tipo de error y su causa probable en una sola frase.
        El problema es: {{question}}
        """)
    @Agent("Diagnosticador técnico")
    String diagnose(@MemoryId String sessionId, @V("question") String question);
}

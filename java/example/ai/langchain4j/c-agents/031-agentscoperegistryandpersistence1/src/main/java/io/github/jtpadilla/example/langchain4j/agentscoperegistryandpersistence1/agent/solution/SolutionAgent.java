package io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent.solution;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SolutionAgent {

    @UserMessage("""
        Eres un experto en resolución de problemas técnicos de software.
        Dado el problema y su diagnóstico, propón una solución concisa (máximo 2 frases).
        Problema: {{question}}
        Diagnóstico: {{diagnosis}}
        """)
    @Agent("Generador de soluciones técnicas")
    String solve(@MemoryId String sessionId, @V("question") String question, @V("diagnosis") String diagnosis);
}

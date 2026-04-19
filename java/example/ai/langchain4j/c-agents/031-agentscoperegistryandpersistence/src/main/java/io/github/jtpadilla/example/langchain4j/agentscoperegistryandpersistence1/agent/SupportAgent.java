package io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;

/**
 * Agente raíz del sistema de soporte multi-agente.
 *
 * Al extender AgenticScopeAccess, expone:
 *   - getAgenticScope(memoryId)   → inspecciona el scope de sesión activo
 *   - evictAgenticScope(memoryId) → elimina la sesión del registro interno
 *
 * Construido con sequenceBuilder (no agentBuilder): los sub-agentes
 * escriben sus salidas en el AgenticScope, activando el registro y el store.
 */
public interface SupportAgent extends AgenticScopeAccess {

    @Agent
    String handle(@MemoryId String sessionId, @V("question") String question);
}

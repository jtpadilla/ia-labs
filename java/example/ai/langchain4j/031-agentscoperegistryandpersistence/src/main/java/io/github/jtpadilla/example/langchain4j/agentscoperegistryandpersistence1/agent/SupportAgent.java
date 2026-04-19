package io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agente raíz del sistema de soporte técnico.
 *
 * Extiende AgenticScopeAccess para exponer:
 *   - getAgenticScope(memoryId)  → inspecciona el scope de una sesión
 *   - evictAgenticScope(memoryId) → elimina la sesión del registro
 *
 * El parámetro @MemoryId hace que el framework mantenga un AgenticScope
 * separado por cada valor de sessionId, activando el registro interno.
 */
public interface SupportAgent extends AgenticScopeAccess {

    @UserMessage("""
        Eres un asistente de soporte técnico especializado en software y sistemas.
        Responde de forma concisa y técnica (máximo 2 frases).
        Recuerda el contexto de esta sesión para dar respuestas coherentes.
        La consulta del usuario es: {{question}}
        """)
    @Agent("Asistente de soporte técnico con memoria de sesión")
    String support(@MemoryId String sessionId, @V("question") String question);
}

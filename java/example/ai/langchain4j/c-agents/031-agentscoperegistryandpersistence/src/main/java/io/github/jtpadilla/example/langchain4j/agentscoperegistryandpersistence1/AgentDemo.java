package io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1;

import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent.SupportAgent;
import io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent.SupportAgentImpl;
import io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.store.LoggingAgenticScopeStore;
import io.github.jtpadilla.example.util.GoogleModels;
import io.helidon.config.Config;

/**
 * Demuestra el registro y la persistencia de AgenticScope en un sistema multi-agente.
 *
 * El sistema es un secuencial de dos sub-agentes:
 *   DiagnosticAgent  → escribe "diagnosis" en el AgenticScope
 *   SolutionAgent    → lee "diagnosis" y escribe "solution" en el AgenticScope
 *
 * Conceptos ilustrados:
 *
 *  1. Store personalizado  — AgenticScopePersister.setStore() conecta una implementación
 *                            propia de AgenticScopeStore (save/load/delete/getAllKeys).
 *                            Solo funciona con sequenceBuilder/supervisorBuilder, no con
 *                            agentBuilder (que es un sub-agente, no un sistema completo).
 *
 *  2. Registro automático  — con chatMemoryProvider + @MemoryId el scope se conserva
 *                            entre invocaciones en vez de descartarse tras cada llamada.
 *
 *  3. Inspección del scope — getAgenticScope(sessionId) accede al scope activo para
 *                            leer las variables de estado escritas por los sub-agentes.
 *
 *  4. Evicción explícita   — evictAgenticScope(sessionId) elimina el scope del registro
 *                            y llama a delete() en el store. Devuelve false si ya no existía.
 */
public class AgentDemo {

    private static final String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("La clave de configuración 'gemini-api-key' es obligatoria"));

    public static void main(String[] args) {

        // ── 1. Registrar store de persistencia ANTES de construir el agente ──
        LoggingAgenticScopeStore store = new LoggingAgenticScopeStore();
        AgenticScopePersister.setStore(store);

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(GoogleModels.geminiFlashLite())
                .build();

        SupportAgent agent = SupportAgentImpl.build(chatModel);

        // ── 2. Dos sesiones con @MemoryId ────────────────────────────────────
        sep("TURNO 1 — primeras consultas");
        ask(agent, "alice", "Mi app Java lanza NullPointerException al iniciar.");
        ask(agent, "bob",   "No consigo conectar a PostgreSQL desde Docker.");

        sep("TURNO 2 — seguimiento (scope con memoria entre turnos)");
        ask(agent, "alice", "El error ocurre en la inicialización del DataSource.");
        ask(agent, "bob",   "Ambos contenedores están en la misma red; el puerto parece bloqueado.");

        // ── 3. Inspeccionar el registro y los scopes ─────────────────────────
        sep("ESTADO DEL REGISTRO");
        System.out.println("Scopes en el store: " + store.getAllKeys().size());
        store.getAllKeys().forEach(k -> System.out.println("  • " + k));

        AgenticScope aliceScope = agent.getAgenticScope("alice");
        System.out.println("\nScope 'alice' activo: " + (aliceScope != null));
        if (aliceScope != null) {
            System.out.println("  Tiene 'diagnosis': " + aliceScope.hasState("diagnosis"));
            System.out.println("  Tiene 'solution':  " + aliceScope.hasState("solution"));
        }

        // ── 4. Evictar scopes cuando las sesiones terminan ───────────────────
        sep("EVICCIÓN DE SESIONES");
        System.out.println("Evictar 'bob':    " + agent.evictAgenticScope("bob"));
        System.out.println("Evictar 'alice':  " + agent.evictAgenticScope("alice"));
        System.out.println("Repetir 'alice':  " + agent.evictAgenticScope("alice")); // ya no existe → false

        sep("ESTADO FINAL");
        System.out.println("Scopes restantes en el store: " + store.getAllKeys().size());
    }

    private static void ask(SupportAgent agent, String sessionId, String question) {
        System.out.printf("%n[%s] %s%n", sessionId, question);
        String solution = agent.handle(sessionId, question);
        System.out.printf("[→%s] %s%n", sessionId, solution);
    }

    private static void sep(String title) {
        System.out.printf("%n────────────── %s ─────────────%n", title);
    }
}

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
 * Demuestra el registro y la persistencia de AgenticScope en LangChain4j.
 *
 * Conceptos ilustrados:
 *
 *  1. Store personalizado  — AgenticScopePersister.setStore() conecta una implementación
 *                            propia de AgenticScopeStore (save/load/delete/getAllKeys).
 *
 *  2. Registro automático  — al usar chatMemoryProvider + @MemoryId, cada sessionId
 *                            obtiene su propio AgenticScope que persiste entre invocaciones.
 *
 *  3. Inspección del scope — getAgenticScope(sessionId) devuelve el AgenticScope activo
 *                            para leer sus variables de estado.
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

        // ── 2. Dos sesiones independientes con @MemoryId ─────────────────────
        //    Cada sessionId tiene su propio AgenticScope en el registro
        sep("TURNO 1 — primeras preguntas de cada usuario");
        ask(agent, "alice", "Mi app Java lanza NullPointerException al iniciar.");
        ask(agent, "bob",   "No consigo conectar a PostgreSQL desde Docker.");

        sep("TURNO 2 — seguimiento (el scope recuerda el contexto anterior)");
        ask(agent, "alice", "El stacktrace apunta a la inicialización del DataSource.");
        ask(agent, "bob",   "El puerto 5432 parece bloqueado. Ambos contenedores están en la misma red.");

        // ── 3. Inspeccionar el registro ───────────────────────────────────────
        sep("ESTADO DEL REGISTRO");
        System.out.println("Scopes en el store: " + store.getAllKeys().size());
        store.getAllKeys().forEach(k -> System.out.println("  • " + k));

        AgenticScope aliceScope = agent.getAgenticScope("alice");
        System.out.println("\nScope de 'alice' activo: " + (aliceScope != null));
        if (aliceScope != null) {
            System.out.println("  Tiene variable 'answer': " + aliceScope.hasState("answer"));
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
        String answer = agent.support(sessionId, question);
        System.out.printf("[→%s] %s%n", sessionId, answer);
    }

    private static void sep(String title) {
        System.out.printf("%n────────────── %s ─────────────%n", title);
    }
}

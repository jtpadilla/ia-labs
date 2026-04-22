package io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.store;

import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación de AgenticScopeStore que persiste los scopes en un mapa en memoria
 * y registra cada operación por consola para observar el ciclo de vida del registro.
 *
 * En un caso real, save/load/delete se mapearían a operaciones de base de datos o sistema de archivos.
 */
public class LoggingAgenticScopeStore implements AgenticScopeStore {

    private final Map<AgenticScopeKey, DefaultAgenticScope> store = new ConcurrentHashMap<>();

    @Override
    public boolean save(AgenticScopeKey key, DefaultAgenticScope agenticScope) {
        store.put(key, agenticScope);
        System.out.printf("[STORE] save()   key=%-25s  total_entries=%d%n", key, store.size());
        return true;
    }

    @Override
    public Optional<DefaultAgenticScope> load(AgenticScopeKey key) {
        DefaultAgenticScope scope = store.get(key);
        System.out.printf("[STORE] load()   key=%-25s  found=%b%n", key, scope != null);
        return Optional.ofNullable(scope);
    }

    @Override
    public boolean delete(AgenticScopeKey key) {
        boolean removed = store.remove(key) != null;
        System.out.printf("[STORE] delete() key=%-25s  removed=%b%n", key, removed);
        return removed;
    }

    @Override
    public Set<AgenticScopeKey> getAllKeys() {
        return Collections.unmodifiableSet(store.keySet());
    }

}

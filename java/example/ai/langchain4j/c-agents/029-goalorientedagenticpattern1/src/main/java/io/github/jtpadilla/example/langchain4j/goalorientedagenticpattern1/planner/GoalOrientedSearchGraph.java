package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.planner;

import dev.langchain4j.agentic.planner.AgentInstance;

import java.util.*;

// Planificador de avance hacia delante (forward-chaining): encadena agentes cuyas entradas
// ya están disponibles hasta alcanzar el goal, equivalente a STRIPS clásico.
public class GoalOrientedSearchGraph {

    private final List<AgentInstance> agents;

    public GoalOrientedSearchGraph(List<AgentInstance> agents) {
        this.agents = agents;
    }

    // Devuelve la secuencia mínima de agentes que produce 'goal' a partir de 'initialKeys'.
    // Itera hasta que no haya progreso (punto fijo) o el goal esté satisfecho.
    public List<AgentInstance> search(Set<String> initialKeys, String goal) {
        Set<String> available = new HashSet<>(initialKeys);
        List<AgentInstance> path = new ArrayList<>();

        boolean progress = true;
        while (progress && !available.contains(goal)) {
            progress = false;
            for (var agent : agents) {
                // Salta agentes cuyo output ya fue producido en una iteración anterior
                if (available.contains(agent.outputKey())) continue;
                boolean satisfied = agent.arguments().stream()
                        .allMatch(arg -> available.contains(arg.name()) || arg.defaultValue() != null);
                if (satisfied) {
                    path.add(agent);
                    available.add(agent.outputKey());
                    progress = true;
                }
            }
        }

        return available.contains(goal) ? path : Collections.emptyList();
    }
}

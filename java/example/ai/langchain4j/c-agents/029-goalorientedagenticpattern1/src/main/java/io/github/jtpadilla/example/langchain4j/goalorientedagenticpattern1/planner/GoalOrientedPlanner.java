package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.planner;

import dev.langchain4j.agentic.planner.*;

import java.util.List;
import java.util.stream.Collectors;

// Planner dirigido por objetivo: resuelve el camino de agentes una vez y lo recorre en orden.
public class GoalOrientedPlanner implements Planner {

    private String goal;

    private GoalOrientedSearchGraph graph;
    // Secuencia de agentes a ejecutar, calculada en firstAction y avanzada con agentCursor
    private List<AgentInstance> path;

    private int agentCursor = 0;

    @Override
    public void init(InitPlanningContext initPlanningContext) {
        // El goal coincide con el outputKey del agente orquestador (p.ej. "writeup")
        this.goal = initPlanningContext.plannerAgent().outputKey();
        this.graph = new GoalOrientedSearchGraph(initPlanningContext.subagents());
    }

    @Override
    public Action firstAction(PlanningContext planningContext) {
        // El estado inicial del scope contiene las claves del input original (p.ej. "prompt")
        path = graph.search(planningContext.agenticScope().state().keySet(), goal);

        // Quiero imprimir la ruta
        String collect = path.stream()
                .map(AgentInstance::name)
                .collect(Collectors.joining("->"));
        System.out.println(collect);

        if (path.isEmpty()) {
            throw new IllegalStateException("No se encontró ningún camino hacia el objetivo: " + goal);
        }
        return call(path.get(agentCursor++));
    }

    @Override
    public Action nextAction(PlanningContext planningContext) {
        return agentCursor >= path.size() ? done() : call(path.get(agentCursor++));
    }

}
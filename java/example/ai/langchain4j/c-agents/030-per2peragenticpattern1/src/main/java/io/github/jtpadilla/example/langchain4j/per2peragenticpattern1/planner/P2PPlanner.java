package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.planner;

import dev.langchain4j.agentic.planner.*;
import dev.langchain4j.agentic.scope.AgenticScope;

import java.util.Map;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toMap;

// Planificador P2P (peer-to-peer): activa en paralelo todos los agentes cuyas entradas
// ya están disponibles en el scope, iterando hasta que se cumpla la condición de salida.
public class P2PPlanner implements Planner {

    private final int maxAgentInvocations;
    private final Predicate<AgenticScope> exitCondition;

    private int invocationCounter = 0;
    private Map<String, AgentActivator> agentActivators;

    public P2PPlanner(int maxAgentInvocations, Predicate<AgenticScope> exitCondition) {
        this.maxAgentInvocations = maxAgentInvocations;
        this.exitCondition = exitCondition;
    }

    @Override
    public void init(InitPlanningContext initPlanningContext) {
        this.agentActivators = initPlanningContext.subagents().stream()
                .collect(toMap(AgentInstance::agentId, AgentActivator::new));
    }

    @Override
    public Action firstAction(PlanningContext planningContext) {
        if (terminated(planningContext.agenticScope())) {
            return done();
        }
        return nextCallAction(planningContext.agenticScope());
    }

    @Override
    public Action nextAction(PlanningContext planningContext) {
        if (terminated(planningContext.agenticScope())) {
            return done();
        }

        AgentActivator last = agentActivators.get(planningContext.previousAgentInvocation().agentId());
        last.finishExecution();
        agentActivators.values().forEach(a -> a.onStateChanged(last.agent().outputKey()));

        return nextCallAction(planningContext.agenticScope());
    }

    private Action nextCallAction(AgenticScope agenticScope) {
        AgentInstance[] agentsToCall = agentActivators.values().stream()
                .filter(a -> a.canActivate(agenticScope))
                .peek(AgentActivator::startExecution)
                .map(AgentActivator::agent)
                .toArray(AgentInstance[]::new);
        invocationCounter += agentsToCall.length;
        return call(agentsToCall);
    }

    private boolean terminated(AgenticScope agenticScope) {
        return invocationCounter > maxAgentInvocations || exitCondition.test(agenticScope);
    }

    // Envuelve un AgentInstance con estado de activación para el ciclo P2P.
    private static class AgentActivator {

        private enum State { IDLE, RUNNING, DONE }

        private final AgentInstance agentInstance;
        private State state = State.IDLE;

        AgentActivator(AgentInstance agentInstance) {
            this.agentInstance = agentInstance;
        }

        AgentInstance agent() {
            return agentInstance;
        }

        // Puede activarse si está IDLE y todas sus entradas requeridas están en el scope.
        boolean canActivate(AgenticScope scope) {
            return state == State.IDLE
                    && agentInstance.arguments().stream()
                            .allMatch(arg -> scope.hasState(arg.name()));
        }

        void startExecution() {
            state = State.RUNNING;
        }

        void finishExecution() {
            state = State.DONE;
        }

        void onStateChanged(String key) {
            // no-op: canActivate re-evalúa sobre el scope en cada ciclo
        }
    }

}

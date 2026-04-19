package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.critic.CriticAgentImpl;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.hypothesis.HypothesisAgentImpl;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.literature.LiteratureAgentImpl;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.scorer.ScorerAgentImpl;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.validation.ValidationAgentImpl;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.planner.P2PPlanner;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.tool.ArxivCrawler;

/**
 * Punto de ensamblaje del agente orquestador basado en planificación P2P (peer-to-peer).
 *
 * <p>Patrón agentico "Peer-to-Peer":
 * <ul>
 *   <li>Cada sub-agente declara qué clave produce (outputKey) y qué claves necesita (@V args).</li>
 *   <li>El {@link P2PPlanner} activa en paralelo todos los sub-agentes cuyas entradas
 *       ya están disponibles en el scope, sin seguir una secuencia predefinida.</li>
 *   <li>El ciclo se repite hasta que la condición de salida se satisfaga
 *       (score ≥ 0.85) o se alcance el máximo de invocaciones.</li>
 * </ul>
 *
 * <p>Grafo de dependencias resuelto dinámicamente:
 * <pre>
 *   [input]
 *     "topic"
 *        │
 *        └──► LiteratureAgent  ──► "researchFindings"
 *                                        │
 *                                        └──► HypothesisAgent ──► "hypothesis"
 *                                                                       │
 *                                          ┌────────────────────────────┤
 *                                          │                            │
 *                                          └──► CriticAgent ──► "critique"
 *                                                                       │
 *                                          ┌────────────────────────────┤
 *                                          │                            │
 *                                          ├──► ValidationAgent ──► "hypothesis" (updated)
 *                                          │
 *                                          └──► ScorerAgent ──► "score"
 *                                                                  │
 *                                                            [exit if ≥ 0.85]
 * </pre>
 */
public class ResearchAgentImpl {

    public static ResearchAgent build(ChatModel chatModel) {
        ArxivCrawler arxivCrawler = new ArxivCrawler();

        return AgenticServices.plannerBuilder(ResearchAgent.class)
                .subAgents(
                        LiteratureAgentImpl.build(chatModel, arxivCrawler),
                        HypothesisAgentImpl.build(chatModel, arxivCrawler),
                        CriticAgentImpl.build(chatModel, arxivCrawler),
                        ValidationAgentImpl.build(chatModel, arxivCrawler),
                        ScorerAgentImpl.build(chatModel, arxivCrawler)
                )
                .outputKey("hypothesis")
                .planner(() -> new P2PPlanner(10, agenticScope -> {
                    if (!agenticScope.hasState("score")) {
                        return false;
                    }
                    double score = agenticScope.readState("score", 0.0);
                    System.out.println("Current hypothesis score: " + score);
                    return score >= 0.85;
                }))
                .build();
    }

}

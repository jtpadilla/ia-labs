package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.horoscopegenerator.HoroscopeGenerator;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.horoscopegenerator.HoroscopeGeneratorImpl;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.personextractor.PersonExtractor;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.personextractor.PersonExtractorImpl;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.signextractor.SignExtractor;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.signextractor.SignExtractorImpl;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.storyfinder.StoryFinder;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.storyfinder.StoryFinderImpl;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.writer.Writer;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.writer.WriterImpl;
import io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.planner.GoalOrientedPlanner;

/**
 * Punto de ensamblaje del agente orquestador basado en planificación dirigida por objetivo.
 *
 * <p>Patrón agentico "Goal-Oriented":
 * <ul>
 *   <li>Cada sub-agente declara qué clave produce (outputKey) y qué claves necesita (argumentos).</li>
 *   <li>El {@link GoalOrientedPlanner} usa {@code GoalOrientedSearchGraph} para calcular,
 *       en tiempo de ejecución, la secuencia mínima de sub-agentes que transforma
 *       el input inicial en el goal ("writeup").</li>
 *   <li>No es necesario codificar el orden a mano: basta con registrar los sub-agentes
 *       y declarar el goal.</li>
 * </ul>
 *
 * <p>Grafo de dependencias resuelto automáticamente:
 * <pre>
 *   [input]
 *     "prompt"
 *        │
 *        ├──► PersonExtractor  ──► "person"  ─────────────────────────┐
 *        │                                                             │
 *        └──► SignExtractor    ──► "sign"                             │
 *                                    │                                │
 *                                    └──► HoroscopeGenerator ──► "horoscope"
 *                                                                     │
 *                                         ┌───────────────────────────┤
 *                                         │                           │
 *                                         └──► StoryFinder ──► "story"│
 *                                                                     │
 *                                              ┌──────────────────────┘
 *                                              │
 *                                              └──► Writer ──► "writeup" [GOAL]
 * </pre>
 */
public class GoalOrientedAgentImpl {

    /**
     * Construye el agente orquestador.
     *
     * <p>Los sub-agentes se registran sin orden: el planner infiere la secuencia
     * correcta comparando las claves disponibles en el estado del scope con los
     * argumentos que cada sub-agente necesita para activarse.
     *
     * @param chatModel modelo LLM compartido por todos los sub-agentes
     * @return agente no tipado invocable con {@code Map<String,Object>} de entrada
     */
    static public UntypedAgent build(ChatModel chatModel) {

        // Cada *Impl.build() registra el outputKey que ese sub-agente produce:
        //   personExtractor   → "person"
        //   signExtractor     → "sign"
        //   horoscopeGenerator→ "horoscope"  (requiere: person, sign)
        //   storyFinder       → "story"      (requiere: person, horoscope)
        //   writer            → "writeup"    (requiere: person, horoscope, story)  ← GOAL
        HoroscopeGenerator horoscopeGenerator = HoroscopeGeneratorImpl.build(chatModel);
        PersonExtractor personExtractor = PersonExtractorImpl.build(chatModel);
        SignExtractor signExtractor = SignExtractorImpl.build(chatModel);
        Writer writer = WriterImpl.build(chatModel);
        StoryFinder storyFinder = StoryFinderImpl.build(chatModel);

        // plannerBuilder ensambla el agente orquestador:
        //   subAgents → pool de sub-agentes disponibles para el planner
        //   outputKey → nombre del goal que el planner debe alcanzar ("writeup")
        //   planner   → estrategia de planificación (GoalOrientedPlanner = forward-chaining)
        return AgenticServices.plannerBuilder()
                .subAgents(horoscopeGenerator, personExtractor, signExtractor, writer, storyFinder)
                .outputKey("writeup")
                .planner(GoalOrientedPlanner::new)
                .build();
    }

}

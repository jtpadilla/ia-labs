# Patrón Agentico Peer-to-Peer con LangChain4j

## Contexto: sistemas multi-agente con LangChain4j

LangChain4j es una librería Java para construir aplicaciones que usan modelos de lenguaje (LLMs).
Su módulo **Agentic** permite componer múltiples llamadas al LLM como si fueran agentes
especializados que se coordinan entre sí. Cada sub-agente es una interfaz Java anotada que el
framework convierte en una llamada al LLM.

Lo esencial en todo sistema multi-agente de LangChain4j Agentic es el **estado compartido**
(`AgenticScope`): un mapa `String → Object` que se va rellenando conforme los agentes se
ejecutan. Cada agente declara:

- **Qué necesita** (`@V("nombre")` en sus parámetros): las claves de entrada que debe encontrar
  en el scope para poder activarse.
- **Qué produce** (`outputKey`): la clave que escribe en el scope cuando termina.

Un **planner** es el componente que decide, en cada ciclo, qué agente(s) invocar a continuación.
Este ejemplo implementa un planner propio: `P2PPlanner`.

---

## El ejemplo: investigación científica iterativa

El programa investiga un tema científico ("agujeros negros") siguiendo un ciclo de
**propuesta → crítica → refinamiento → puntuación**:

1. Busca artículos en arXiv y resume los hallazgos.
2. Formula una hipótesis a partir de esos hallazgos.
3. Critica la hipótesis.
4. Valida (y posiblemente reformula) la hipótesis a la luz de la crítica.
5. Puntúa la hipótesis de 0.0 a 1.0.
6. Si la puntuación es ≥ 0.85, termina. Si no, la condición de salida falla.

---

## Los cinco sub-agentes y sus dependencias

Cada sub-agente es una interfaz Java anotada con `@Agent`, `@SystemMessage` y `@UserMessage`.
El framework genera en tiempo de ejecución un proxy que llama al LLM con esas instrucciones,
sustituyendo las variables `{{nombre}}` por los valores actuales del scope.

| Sub-agente | Necesita (claves de entrada) | Produce (outputKey) |
|---|---|---|
| `LiteratureAgent` | `topic` | `researchFindings` |
| `HypothesisAgent` | `topic`, `researchFindings` | `hypothesis` |
| `CriticAgent` | `topic`, `hypothesis` | `critique` |
| `ValidationAgent` | `topic`, `hypothesis`, `critique` | `hypothesis` (actualizada) |
| `ScorerAgent` | `topic`, `hypothesis`, `critique` | `score` |

El estado inicial contiene solo `topic` ("agujeros negros"), inyectado cuando se invoca
`researcher.research("agujeros negros")`.

---

## El patrón P2P: activación dirigida por dependencias

A diferencia del patrón goal-oriented (ejemplo 029), que calcula el orden de antemano y lo
sigue de forma estricta, el patrón **Peer-to-Peer** no tiene una secuencia predefinida.

La regla es simple: **un agente se activa en cuanto todas sus entradas están disponibles en
el scope**. Varios agentes pueden estar listos al mismo tiempo y activarse en paralelo.

Esto lo gestiona el `P2PPlanner` mediante una máquina de estados por agente (`AgentActivator`):

```
IDLE  ──(canActivate)──►  RUNNING  ──(finishExecution)──►  DONE
```

- `IDLE`: el agente aún no ha sido invocado.
- `RUNNING`: el framework está ejecutando la llamada al LLM.
- `DONE`: el agente terminó y su output está en el scope.

En cada ciclo el planner filtra los activadores en estado `IDLE` cuyas entradas requeridas
están presentes en el scope, los marca como `RUNNING` y los devuelve al framework para
invocarlos (potencialmente en paralelo):

```java
AgentInstance[] agentsToCall = agentActivators.values().stream()
        .filter(a -> a.canActivate(agenticScope))   // IDLE + entradas satisfechas
        .peek(AgentActivator::startExecution)        // IDLE -> RUNNING
        .map(AgentActivator::agent)
        .toArray(AgentInstance[]::new);
return call(agentsToCall);                           // pueden lanzarse en paralelo
```

### Traza de activación sobre el ejemplo

Con el estado inicial `{ topic }`, la secuencia de activación queda así:

```
Ciclo 1 – scope: { topic }
  → LiteratureAgent se activa (solo necesita topic)
    scope: { topic, researchFindings }

Ciclo 2 – scope: { topic, researchFindings }
  → HypothesisAgent se activa (tiene topic + researchFindings)
    scope: { topic, researchFindings, hypothesis }

Ciclo 3 – scope: { topic, researchFindings, hypothesis }
  → CriticAgent se activa (tiene topic + hypothesis)
    scope: { topic, researchFindings, hypothesis, critique }

Ciclo 4 – scope: { topic, researchFindings, hypothesis, critique }
  → ValidationAgent se activa (tiene topic + hypothesis + critique) → actualiza hypothesis
  → ScorerAgent    se activa (tiene topic + hypothesis + critique) → escribe score
    [estos dos pueden ejecutarse en paralelo]
    scope: { topic, researchFindings, hypothesis, critique, score }

Ciclo 5 – terminated() comprueba: score >= 0.85?
  → SÍ: done()
  → NO: invocaciones agotadas (máximo 10)
```

> En este ejemplo concreto, la topología de dependencias hace que solo un agente esté
> listo por ciclo hasta el ciclo 4, donde `ValidationAgent` y `ScorerAgent` pueden
> activarse en paralelo al tener los mismos requisitos.

---

## La condición de terminación

El planner termina antes de invocar al siguiente lote de agentes si:

```java
private boolean terminated(AgenticScope agenticScope) {
    return invocationCounter > maxAgentInvocations
        || exitCondition.test(agenticScope);
}
```

La condición concreta se define en `ResearchAgentImpl`:

```java
new P2PPlanner(10, agenticScope -> {
    if (!agenticScope.hasState("score")) return false;
    double score = agenticScope.readState("score", 0.0);
    System.out.println("Current hypothesis score: " + score);
    return score >= 0.85;
})
```

- Si `score` todavía no está en el scope (antes del ciclo 5), devuelve `false` y el planner
  continúa.
- Si `score >= 0.85`, termina con éxito devolviendo la clave `"hypothesis"` del scope
  como resultado final.
- El límite de 10 invocaciones actúa como cortocircuito de seguridad.

---

## La herramienta ArxivCrawler

LangChain4j permite registrar **tools** (herramientas) en un agente. Una tool es un método
Java anotado con `@Tool` que el LLM puede decidir llamar cuando lo necesita, igual que
*function calling* de OpenAI o *tool use* de Claude.

`ArxivCrawler` expone dos tools:

```java
@Tool("Busca artículos científicos en arxiv.org...")
public String searchPapers(@P("...") String query, @P("...") int maxResults)

@Tool("Obtiene el resumen y los metadatos de un artículo de arXiv a partir de su ID...")
public String getPaper(@P("...") String arxivId)
```

Internamente usa `HttpClient` de Java 21 para llamar a la API pública de arXiv
(`export.arxiv.org/api/query`) y parsea la respuesta Atom/XML con `DocumentBuilderFactory`
(ambas pertenecen al JDK estándar, sin dependencias extra).

El mismo objeto `ArxivCrawler` se comparte entre todos los sub-agentes que lo necesitan
(se instancia una sola vez en `ResearchAgentImpl`).

---

## Flujo de ejecución completo

```
AgentDemo.main()
    |
    +- Construye ChatModel (Gemini Flash Lite, thinking activado)
    |
    +- ResearchAgentImpl.build(chatModel)
    |     +- new ArxivCrawler()
    |     +- LiteratureAgentImpl.build(...)   outputKey="researchFindings"
    |     +- HypothesisAgentImpl.build(...)   outputKey="hypothesis"
    |     +- CriticAgentImpl.build(...)       outputKey="critique"
    |     +- ValidationAgentImpl.build(...)   outputKey="hypothesis"
    |     +- ScorerAgentImpl.build(...)       outputKey="score"
    |     +- AgenticServices.plannerBuilder(ResearchAgent.class)
    |           .subAgents(los 5 anteriores)
    |           .outputKey("hypothesis")      <- resultado final
    |           .planner(() -> new P2PPlanner(10, exitCondition))
    |           .build()  -> ResearchAgent (proxy generado)
    |
    +- researcher.research("agujeros negros")
          |
          +- P2PPlanner.init()
          |     -> crea un AgentActivator (IDLE) por cada sub-agente
          |
          +- P2PPlanner.firstAction()
          |     -> nextCallAction(): LiteratureAgent está IDLE y tiene "topic"
          |     -> call(literatureAgent)  [LLM llama a searchPapers()]
          |
          +- P2PPlanner.nextAction() x N
          |     -> marca al anterior como DONE
          |     -> nextCallAction(): activa los siguientes con dependencias satisfechas
          |     -> en el último ciclo: terminated() devuelve true -> done()
          |
          +- Framework extrae scope["hypothesis"] y lo devuelve como String
    |
    +- System.out.println(Format.markdown(hypothesis))
```

---

## Diferencia clave respecto al patrón Goal-Oriented (ejemplo 029)

| | Goal-Oriented (029) | Peer-to-Peer (030) |
|---|---|---|
| Orden de ejecución | Calculado de antemano por BFS/forward-chaining | Emergente: cada agente se activa cuando sus entradas están listas |
| Paralelismo | Secuencial (un agente cada vez) | Posible: varios agentes pueden activarse en el mismo ciclo |
| Terminación | Cuando se alcanza el goal | Por condición explícita sobre el scope (p.ej. puntuación) o límite de invocaciones |
| Mejor para | Pipelines con dependencias lineales claras | Grafos de dependencias con ramas paralelas o bucles de refinamiento |

---

## Estructura de clases

```
AgentDemo                          <- punto de entrada: construye modelo, invoca, imprime
|
+-- agent/
|     +-- ResearchAgent            <- interfaz del orquestador (entrada: topic, salida: hypothesis)
|     +-- ResearchAgentImpl        <- ensambla sub-agentes + P2PPlanner
|     |
|     +-- literature/
|     |     LiteratureAgent + LiteratureAgentImpl   (outputKey: "researchFindings")
|     +-- hypothesis/
|     |     HypothesisAgent + HypothesisAgentImpl   (outputKey: "hypothesis")
|     +-- critic/
|     |     CriticAgent + CriticAgentImpl           (outputKey: "critique")
|     +-- validation/
|     |     ValidationAgent + ValidationAgentImpl   (outputKey: "hypothesis")
|     +-- scorer/
|           ScorerAgent + ScorerAgentImpl           (outputKey: "score")
|
+-- planner/
|     P2PPlanner                   <- Planner: ciclo firstAction / nextAction
|         AgentActivator           <- máquina de estados IDLE/RUNNING/DONE (inner class)
|
+-- tool/
      ArxivCrawler                 <- tool con @Tool: busca y recupera papers de arXiv
```

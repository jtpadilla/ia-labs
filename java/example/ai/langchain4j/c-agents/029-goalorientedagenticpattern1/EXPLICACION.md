# Patrón Agentico Goal-Oriented con LangChain4j

## ¿Qué es LangChain4j?

LangChain4j es una librería Java para construir aplicaciones que usan modelos de lenguaje (LLMs).
Entre sus abstracciones más recientes está el módulo **Agentic**, que permite componer
múltiples llamadas al LLM como si fueran agentes especializados que se coordinan entre sí.

La idea central es: en lugar de un único prompt enorme que haga todo, se divide el problema
en **sub-agentes** pequeños, cada uno con una responsabilidad concreta. Un **planner** decide
el orden en que se invocan.

---

## El ejemplo: generador de horóscopo personalizado

El programa recibe una frase en lenguaje natural como:

```
"Me llamo Mario y mi signo zodiacal es piscis"
```

Y produce un texto divertido que combina el horóscopo de Mario con una noticia ficticia.

Para llegar ahí encadena cinco sub-agentes, cada uno resolviendo un paso del problema.

---

## Concepto clave: sub-agente como función con nombre

En LangChain4j Agentic, un sub-agente es una interfaz Java anotada que el framework
convierte en una llamada al LLM. Lo importante para el planner son dos cosas:

- **Qué necesita** (sus parámetros, con nombre): las claves de entrada.
- **Qué produce** (su `outputKey`): la clave de salida que deja en el estado compartido.

El estado compartido es simplemente un `Map<String, Object>` que se va rellenando
conforme los agentes se ejecutan.

### Los cinco sub-agentes de este ejemplo

| Sub-agente | Necesita | Produce |
|---|---|---|
| `PersonExtractor` | `"prompt"` | `"person"` |
| `SignExtractor` | `"prompt"` | `"sign"` |
| `HoroscopeGenerator` | `"person"`, `"sign"` | `"horoscope"` |
| `StoryFinder` | `"person"`, `"horoscope"` | `"story"` |
| `Writer` | `"person"`, `"horoscope"`, `"story"` | `"writeup"` <- goal |

El estado inicial contiene solo `"prompt"`. El goal es `"writeup"`.

---

## El planner: planificación por avance hacia delante (forward-chaining)

Este es el núcleo del patrón. El `GoalOrientedPlanner` no tiene el orden de ejecución
codificado a mano. Lo calcula solo, usando el algoritmo implementado en `GoalOrientedSearchGraph`.

### Algoritmo forward-chaining

Es equivalente al sistema de planificación **STRIPS** clásico de IA simbólica:

1. Se parte de un conjunto de **hechos disponibles** (las claves del estado inicial).
2. En cada iteración se busca algún agente cuyos inputs estén todos disponibles y cuyo
   output todavía no lo esté.
3. Ese agente se añade al plan y su output se incorpora a los hechos disponibles.
4. Se repite hasta que el goal esté entre los hechos disponibles, o hasta que no haya
   progreso (error: goal inalcanzable).

### Traza de ejecución sobre el ejemplo

```
Estado inicial: { "prompt" }

Iteración 1:
  PersonExtractor    necesita: prompt     -> añade "person"
  SignExtractor      necesita: prompt     -> añade "sign"
  HoroscopeGenerator necesita: person, sign -> añade "horoscope"
  StoryFinder        necesita: person, horoscope -> añade "story"
  Writer             necesita: person, horoscope, story -> añade "writeup" GOAL

Plan calculado: PersonExtractor -> SignExtractor -> HoroscopeGenerator -> StoryFinder -> Writer
```

> El código imprime esta ruta en consola gracias a la línea de debug en `GoalOrientedPlanner.firstAction()`.

### Por qué es útil este patrón

Si mañana se añade un nuevo sub-agente (p.ej. un `TranslatorAgent` que requiere `"writeup"`
y produce `"writeup_translated"`), basta con registrarlo. El planner re-calcula el orden
automáticamente sin tocar el resto del código.

---

## Flujo de ejecución paso a paso

```
AgentDemo.main()
    |
    +- Construye el ChatModel (Gemini Flash Lite con thinking activado)
    |
    +- GoalOrientedAgentImpl.build(chatModel)
    |     +- Crea los 5 sub-agentes (cada uno envuelve una interfaz anotada)
    |     +- AgenticServices.plannerBuilder()
    |           .subAgents(...)            <- pool de agentes disponibles
    |           .outputKey("writeup")      <- goal
    |           .planner(GoalOrientedPlanner::new)  <- estrategia
    |           .build()  -> UntypedAgent
    |
    +- horoscopeAgent.invoke({"prompt": "Me llamo Mario..."})
          |
          +- GoalOrientedPlanner.init()        -> crea el grafo de busqueda
          +- GoalOrientedPlanner.firstAction()
          |     +- graph.search({"prompt"}, "writeup")  -> calcula el plan
          |     +- invoca PersonExtractor(prompt)  -> "person": Person("Mario", null)
          +- nextAction() -> invoca SignExtractor(prompt)        -> "sign": PISCIS
          +- nextAction() -> invoca HoroscopeGenerator(person, sign)  -> "horoscope": "..."
          +- nextAction() -> invoca StoryFinder(person, horoscope)    -> "story": "..."
          +- nextAction() -> invoca Writer(person, horoscope, story)  -> "writeup": "..."
          +- nextAction() -> done()   (agentCursor >= path.size())
```

---

## Estructura de clases

```
AgentDemo                          <- punto de entrada
|
+-- GoalOrientedAgentImpl          <- ensamblaje del agente orquestador
|     +-- GoalOrientedPlanner      <- Planner: ciclo firstAction / nextAction
|     +-- GoalOrientedSearchGraph  <- algoritmo forward-chaining
|
+-- Sub-agentes (interfaz + impl)
|     +-- PersonExtractor   / PersonExtractorImpl
|     +-- SignExtractor     / SignExtractorImpl
|     +-- HoroscopeGenerator / HoroscopeGeneratorImpl
|     +-- StoryFinder       / StoryFinderImpl  (usa WebSearchTool simulado)
|     +-- Writer            / WriterImpl
|
+-- Dominio
      +-- Person(name, horoscope)
      +-- Sign  (enum con los 12 signos)
```

---

## Nota sobre StoryFinder

`StoryFinderImpl` registra un `WebSearchTool` con resultado hardcodeado. En un entorno real
se sustituiria por una API de busqueda (Tavily, Google Custom Search, etc.). El agente
llama a la herramienta exactamente una vez (lo indica el `@SystemMessage`) y usa el resultado
para construir su output.

---

## Nota sobre el record `Person`

```java
public record Person(String name, String horoscope) {}
```

El campo `horoscope` no lo rellena `PersonExtractor` (que solo extrae el nombre del texto libre).
Lo utiliza el modelo cuando `HoroscopeGenerator` devuelve su resultado y el framework
reconstruye la entidad. En la practica, durante la extraccion inicial el campo llega a `null`.

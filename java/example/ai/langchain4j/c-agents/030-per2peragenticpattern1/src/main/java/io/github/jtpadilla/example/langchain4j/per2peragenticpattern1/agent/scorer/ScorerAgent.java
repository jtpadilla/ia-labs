package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.scorer;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ScorerAgent {

    @SystemMessage("Puntúa la hipótesis proporcionada sobre el tema dado, basándote en la crítica recibida.")
    @UserMessage("""
            Eres un agente de puntuación.
            Tu tarea es puntuar la hipótesis proporcionada por el usuario en relación con el tema especificado, basándote en la crítica recibida.
            Puntúa la hipótesis en una escala de 0.0 a 1.0, donde 0.0 significa que la hipótesis es completamente inválida y 1.0 que es totalmente válida.
            El tema es: {{topic}}
            La hipótesis es: {{hypothesis}}
            La crítica es: {{critique}}
            """)
    @Agent("Puntúa una hipótesis a partir de un tema dado y su crítica")
    double scoreHypothesis(@V("topic") String topic, @V("hypothesis") String hypothesis, @V("critique") String critique);

}

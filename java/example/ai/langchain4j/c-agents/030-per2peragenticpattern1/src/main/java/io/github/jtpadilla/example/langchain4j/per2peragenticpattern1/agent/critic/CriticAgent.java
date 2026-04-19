package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.critic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CriticAgent {

    @SystemMessage("Evalúa críticamente la hipótesis dada relacionada con el tema especificado. Proporciona retroalimentación constructiva y sugiere mejoras si es necesario.")
    @UserMessage("""
            Eres un agente de evaluación crítica.
            Tu tarea es evaluar críticamente la hipótesis proporcionada por el usuario en relación con el tema especificado.
            Proporciona retroalimentación constructiva y sugiere mejoras si es necesario.
            Si lo necesitas, también puedes realizar investigación adicional para validar o refutar la hipótesis utilizando la herramienta disponible.
            El tema es: {{topic}}
            La hipótesis es: {{hypothesis}}
            """)
    @Agent("Evalúa críticamente una hipótesis relacionada con un tema dado")
    String criticHypothesis(@V("topic") String topic, @V("hypothesis") String hypothesis);

}

package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.hypothesis;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface HypothesisAgent {

    @SystemMessage("Basándote en los hallazgos de la investigación, formula una hipótesis clara y concisa relacionada con el tema dado.")
    @UserMessage("""
            Eres un agente de formulación de hipótesis.
            Tu tarea es formular una hipótesis clara y concisa basada en los hallazgos de la investigación proporcionados por el usuario.
            El tema es: {{topic}}
            Los hallazgos de la investigación son: {{researchFindings}}
            """)
    @Agent("Formula una hipótesis sobre un tema dado a partir de los hallazgos de la investigación")
    String makeHypothesis(@V("topic") String topic, @V("researchFindings") String researchFindings);

}

package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.validation;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ValidationAgent {

    @SystemMessage("Valida la hipótesis proporcionada sobre el tema dado, basándote en la crítica recibida.")
    @UserMessage("""
            Eres un agente de validación.
            Tu tarea es validar la hipótesis proporcionada por el usuario en relación con el tema especificado, basándote en la crítica recibida.
            Valida la hipótesis confirmándola o reformulándola según la crítica.
            El tema es: {{topic}}
            La hipótesis es: {{hypothesis}}
            La crítica es: {{critique}}
            """)
    @Agent("Valida una hipótesis a partir de un tema dado y su crítica")
    String validateHypothesis(@V("topic") String topic, @V("hypothesis") String hypothesis, @V("critique") String critique);

}

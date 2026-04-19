package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.literature;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LiteratureAgent {

    @SystemMessage("Busca literatura científica sobre el tema indicado y devuelve un resumen de los hallazgos.")
    @UserMessage("""
            Eres un agente de búsqueda de literatura científica.
            Tu tarea es encontrar artículos científicos relevantes sobre el tema proporcionado por el usuario y resumirlos.
            Utiliza la herramienta disponible para buscar artículos científicos y devuelve un resumen de tus hallazgos.
            El tema es: {{topic}}
            """)
    @Agent("Busca literatura científica sobre un tema dado")
    String searchLiterature(@V("topic") String topic);

}

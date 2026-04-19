package io.github.jtpadilla.example.langchain4j.goalorientedagenticpattern1.agent.storyfinder;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.web.search.WebSearchInformationResult;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.WebSearchTool;

import java.net.URI;
import java.util.List;

public class StoryFinderImpl {

    static public StoryFinder build(ChatModel chatModel) {
        return AgenticServices
                .agentBuilder(StoryFinder.class)
                .chatModel(chatModel)
                // WebSearchTool con resultado hardcodeado: evita dependencia de una API de búsqueda real en el demo
                .tools(WebSearchTool.from(request -> WebSearchResults.from(
                        WebSearchInformationResult.from(1L),
                        List.of(WebSearchOrganicResult.from(
                                "Un Sagitario intentó predecir el futuro con una bola de cristal y acabó pidiendo pizza",
                                URI.create("https://example.com/funny-story"),
                                "Noticias ficticias del universo",
                                "Un astrólogo sagitariano aseguró haber visto el futuro en su bola de cristal, pero sólo vio su reflejo pidiendo una pizza de pepperoni a las 3am."
                        ))
                )))
                .outputKey("story")
                .build();
    }

}

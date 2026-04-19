package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.literature;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.tool.ArxivCrawler;

public class LiteratureAgentImpl {

    public static LiteratureAgent build(ChatModel chatModel, ArxivCrawler arxivCrawler) {
        return AgenticServices.agentBuilder(LiteratureAgent.class)
                .chatModel(chatModel)
                .tools(arxivCrawler)
                .outputKey("researchFindings")
                .build();
    }

}

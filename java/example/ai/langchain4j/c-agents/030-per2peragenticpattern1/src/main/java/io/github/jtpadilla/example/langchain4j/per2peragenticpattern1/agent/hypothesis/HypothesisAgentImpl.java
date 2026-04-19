package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.hypothesis;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.tool.ArxivCrawler;

public class HypothesisAgentImpl {

    public static HypothesisAgent build(ChatModel chatModel, ArxivCrawler arxivCrawler) {
        return AgenticServices.agentBuilder(HypothesisAgent.class)
                .chatModel(chatModel)
                .tools(arxivCrawler)
                .outputKey("hypothesis")
                .build();
    }

}

package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.agent.critic;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.tool.ArxivCrawler;

public class CriticAgentImpl {

    public static CriticAgent build(ChatModel chatModel, ArxivCrawler arxivCrawler) {
        return AgenticServices.agentBuilder(CriticAgent.class)
                .chatModel(chatModel)
                .tools(arxivCrawler)
                .outputKey("critique")
                .build();
    }

}

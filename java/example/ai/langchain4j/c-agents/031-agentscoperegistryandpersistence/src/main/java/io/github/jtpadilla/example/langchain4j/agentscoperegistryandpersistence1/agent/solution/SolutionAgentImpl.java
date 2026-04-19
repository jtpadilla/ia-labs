package io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent.solution;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;

public class SolutionAgentImpl {

    public static SolutionAgent build(ChatModel chatModel) {
        return AgenticServices.agentBuilder(SolutionAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("solution")
                .build();
    }
}

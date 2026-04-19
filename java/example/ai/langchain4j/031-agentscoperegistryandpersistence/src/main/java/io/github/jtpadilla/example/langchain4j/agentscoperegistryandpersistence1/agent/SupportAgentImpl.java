package io.github.jtpadilla.example.langchain4j.agentscoperegistryandpersistence1.agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;

public class SupportAgentImpl {

    public static SupportAgent build(ChatModel chatModel) {
        return AgenticServices.agentBuilder(SupportAgent.class)
                .chatModel(chatModel)
                // chatMemoryProvider activa el registro de AgenticScope por sessionId:
                // cada @MemoryId distinto obtiene su propio ChatMemory y su propio AgenticScope
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("answer")
                .build();
    }
}

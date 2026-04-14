package io.github.jtpadilla.example.langchain4j.sequentialworkflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.helidon.config.Config;

public class AgentDemo {

    final static private String MODEL = "gemini-3.1-flash-lite-preview";

    final static private String API_KEY = Config.global().get("gemini-api-key").asString().orElseThrow(
            () -> new IllegalStateException("Configuration key 'gemini-api-key' is required"));

    public static void main(String[] args) {

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(API_KEY)
                .modelName(MODEL)
                .logRequestsAndResponses(true)
                .build();

        interface CreativeWriter {
            @UserMessage("""
            You are a creative writer.
            Generate a draft of a story no more than
            3 sentences long around the given topic.
            Return only the story and nothing else.
            The topic is {{topic}}.
            """)
            @Agent("Generates a story based on the given topic")
            String generateStory(@V("topic") String topic);
        }

        interface AudienceEditor {
            @UserMessage("""
            You are a professional editor.
            Analyze and rewrite the following story to better align
            with the target audience of {{audience}}.
            Return only the story and nothing else.
            The story is "{{story}}".
            """)
            @Agent("Edits a story to better fit a given audience")
            String editStory(@V("story") String story, @V("audience") String audience);
        }

        interface StyleEditor {
            @UserMessage("""
            You are a professional editor.
            Analyze and rewrite the following story to better fit and be more coherent with the {{style}} style.
            Return only the story and nothing else.
            The story is "{{story}}".
            """)
            @Agent("Edits a story to better fit a given style")
            String editStory(@V("story") String story, @V("style") String style);
        }



    }

}
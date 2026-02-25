package io.github.jtpadilla.example.interactions.util;

import io.github.glaforge.gemini.interactions.model.Content;
import io.github.glaforge.gemini.interactions.model.Interaction;

import java.util.List;

public class Util {

    public static List<Content.ThoughtContent> getThoughts(Interaction interaction) {
        return interaction.outputs().stream()
                .filter(Content.ThoughtContent.class::isInstance)
                .map(Content.ThoughtContent.class::cast)
                .toList();
    }

    public static void dumpThoughts(List<Content.ThoughtContent> thoughts) {
        System.out.println("Thoughts:");
        for (Content.ThoughtContent thought : thoughts) {
            System.out.println("Thought signature: " + thought.signature());
            System.out.println("Thought summary: " + thought.summary());
            if (thought.summary() != null && !thought.summary().isEmpty() ) {
                System.out.println("Thought items:");
                for (Content item : thought.summary()) {
                    if (item instanceof Content.TextContent textContent) {
                        System.out.println("  Text: " + textContent.text());
                    } else if (item instanceof Content.ImageContent imageContent) {
                        System.out.println("  Image: " + imageContent.uri());
                    } else {
                        System.out.println("  Unknown item type: " + item.type());
                    }
                }
            }
        }
    }

    public static List<Content.TextContent> getText(Interaction interaction) {
        System.out.println("Text:");
        return interaction.outputs().stream()
                .filter(Content.TextContent.class::isInstance)
                .map(Content.TextContent.class::cast)
                .toList();
    }

    public static void dumpText(List<Content.TextContent> text) {
        for (Content.TextContent content : text) {
            System.out.println(content.text());
        }
    }

}

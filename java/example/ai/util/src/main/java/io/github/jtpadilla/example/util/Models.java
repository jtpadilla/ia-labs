package io.github.jtpadilla.example.util;

public record Models(String supervisor, String agent) {

    // Supervisor con thinking + sub-agentes con el mismo modelo (sin thinking en sub-agentes)
    public static Models geminiFlashLite() {
        return new Models("gemini-3.1-flash-lite-preview", "gemini-3.1-flash-lite-preview");
    }

    // Supervisor con thinking (Gemini) + sub-agentes sin thinking (Gemma 26b)
    public static Models geminiSupervisorGemma26bAgents() {
        return new Models("gemini-3.1-flash-lite-preview", "gemma-4-26b-a4b-it");
    }

    // Supervisor y sub-agentes con Gemma 26b (~1 min 35 s)
    public static Models gemma26b() {
        return new Models("gemma-4-26b-a4b-it", "gemma-4-26b-a4b-it");
    }

    // Supervisor y sub-agentes con Gemma 31b (~1 min 32 s)
    public static Models gemma31b() {
        return new Models("gemma-4-31b-it", "gemma-4-31b-it");
    }

}

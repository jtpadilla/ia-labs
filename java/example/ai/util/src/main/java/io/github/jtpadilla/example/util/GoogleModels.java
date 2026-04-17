package io.github.jtpadilla.example.util;

public record GoogleModels(String supervisor, String agent) {

    // Supervisor con thinking + sub-agentes con el mismo modelo (sin thinking en sub-agentes)
    public static GoogleModels geminiFlashLite() {
        return new GoogleModels("gemini-3.1-flash-lite-preview", "gemini-3.1-flash-lite-preview");
    }

    // Supervisor con thinking (Gemini) + sub-agentes sin thinking (Gemma 26b) -> 37.9s
    public static GoogleModels geminiSupervisorGemma26bAgents() {
        return new GoogleModels("gemini-3.1-flash-lite-preview", "gemma-4-26b-a4b-it");
    }

    // Supervisor y sub-agentes con Gemma 26b -> 1m 45s
    public static GoogleModels gemma26b() {
        return new GoogleModels("gemma-4-26b-a4b-it", "gemma-4-26b-a4b-it");
    }

    // Supervisor y sub-agentes con Gemma 31b (~1 min 32 s)
    public static GoogleModels gemma31b() {
        return new GoogleModels("gemma-4-31b-it", "gemma-4-31b-it");
    }

}

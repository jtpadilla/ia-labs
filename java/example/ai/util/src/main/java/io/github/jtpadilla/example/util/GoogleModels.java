package io.github.jtpadilla.example.util;

public class GoogleModels {

    // Supervisor con thinking + sub-agentes con el mismo modelo (sin thinking en sub-agentes)
    public static String geminiFlashLite() {
        return "gemini-3.1-flash-lite-preview";
    }

    // Supervisor con thinking (Gemini) + sub-agentes sin thinking (Gemma 26b) -> 37.9s
    public static String gemma_4_26b_a4b_it() {
        return "gemma-4-26b-a4b-it";
    }

    // Supervisor y sub-agentes con Gemma 31b (~1 min 32 s)
    public static String  gemma4_31b_it() {
        return "gemma-4-31b-it";
    }

}

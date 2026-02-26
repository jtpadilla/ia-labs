package io.github.jtpadilla.example.interactions.demo.researchfrontend.impl;

import io.javelit.core.Jt;

public class DisplayError {

    static public void javelit() {
        Jt.error("GEMINI_API_KEY environment variable not set").use();
    }

}

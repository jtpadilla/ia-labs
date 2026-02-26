package io.github.jtpadilla.example.javelit.widgetcheckboxoptions;

import io.javelit.core.Jt;
import io.javelit.core.Server;

import java.util.List;

class WidgetCheckBoxOptions {

    static void main(String[] args) {
        Server.builder(io.github.jtpadilla.example.javelit.widgetcheckboxoptions.WidgetCheckBoxOptions::javelit, 8080)
                .build()
                .start();
    }

    static private void javelit() {
        String option = Jt.selectbox(
                "Which fruit do you like best?",
                List.of("Apple", "Banana", "Kiwi")).use();

        Jt.text("You selected: " + option).use();

    }

}
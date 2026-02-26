package io.github.jtpadilla.example.javelit.layout2;

import io.javelit.core.Jt;
import io.javelit.core.Server;

import java.util.List;

class Layout2 {

    static void main(String[] args) {
        Server.builder(Layout2::javelit, 8080)
                .build()
                .start();
    }

    static private void javelit() {
        var columns = Jt.columns(2).use();

        // You can use a column by passing it to .use():
        Jt.button("Press me!").use(columns.col(0));

        // Place multiple elements in a column:
        String chosen = Jt.radio(
                "Sorting hat",
                List.of("Gryffindor", "Ravenclaw", "Hufflepuff", "Slytherin")
        ).use(columns.col(1));

        Jt.text("You are in " + chosen + " house!").use(columns.col(1));
    }

}
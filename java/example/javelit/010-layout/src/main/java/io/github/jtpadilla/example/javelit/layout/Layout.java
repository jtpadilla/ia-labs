package io.github.jtpadilla.example.javelit.layout;

import io.javelit.core.Jt;
import io.javelit.core.Server;

import java.util.List;

class Layout {

    static void main(String[] args) {
        Server.builder(Layout::javelit, 8080)
                .build()
                .start();
    }

    static private void javelit() {

        // Add a selectbox to the sidebar:
        String selection = Jt
                .selectbox("How would you like to be contacted?", List.of("Email", "Home phone", "Mobile phone"))
                .use(Jt.SIDEBAR);

        // Add a slider to the sidebar:
        double value = Jt.slider("Select a value").min(0.0).max(100.0).value(50).use(Jt.SIDEBAR);

    }

}
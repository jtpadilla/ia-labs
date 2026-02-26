package io.github.jtpadilla.example.javelit.widgetsslider;

import io.javelit.core.Jt;
import io.javelit.core.Server;

class WidgetSlider {

    static void main(String[] args) {
        Server.builder(WidgetSlider::javelit, 8080)
                .build()
                .start();

    }

    static private void javelit() {
        var x = Jt.slider("My slider").use();  // 👈 this is a widget
        Jt.text(x + " squared is " + x * x).use();
    }

}
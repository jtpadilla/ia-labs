package io.github.jtpadilla.example.javelit.drawlinechart;

import io.javelit.core.Jt;
import io.javelit.core.Server;
import org.icepear.echarts.Line;

class DrawLineChart {

    static void main(String[] args) {
        Server.builder(DrawLineChart::javelit, 8080)
                .build()
                .start();

    }

    static private void javelit() {
        Line line = new Line()
                .addXAxis(new String[] { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" })
                .addYAxis()
                .addSeries(new Number[] { 150, 230, 224, 218, 135, 147, 260 });

        Jt.echarts(line).use();
    }

}
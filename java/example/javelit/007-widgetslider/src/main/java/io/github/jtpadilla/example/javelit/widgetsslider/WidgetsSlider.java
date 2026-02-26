package io.github.jtpadilla.example.javelit.widgetsslider;

import io.javelit.core.Jt;
import io.javelit.core.Server;
import org.icepear.echarts.Gauge;
import org.icepear.echarts.charts.gauge.GaugeDataItem;
import org.icepear.echarts.charts.gauge.GaugeProgress;
import org.icepear.echarts.charts.gauge.GaugeSeries;

class WidgetsSlider {

    static void main(String[] args) {
        Server.builder(WidgetsSlider::javelit, 8080)
                .build()
                .start();

    }

    static private void javelit() {
        var x = Jt.slider("My slider").use();  // 👈 this is a widget
        Jt.text(x + " squared is " + x * x).use();
    }

}
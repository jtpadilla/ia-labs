package io.github.jtpadilla.example.javelit.drawgauge;

import io.javelit.core.Jt;
import io.javelit.core.Server;
import org.icepear.echarts.Gauge;
import org.icepear.echarts.charts.gauge.GaugeDataItem;
import org.icepear.echarts.charts.gauge.GaugeProgress;
import org.icepear.echarts.charts.gauge.GaugeSeries;

class DrawGauge {

    static void main(String[] args) {
        Server.builder(DrawGauge::new, 8080)
                .build()
                .start();

    }

    public DrawGauge() {
        Gauge gauge = new Gauge()
                .setTooltip("item")
                .addSeries(new GaugeSeries()
                        .setName("Pressure")
                        .setProgress(new GaugeProgress().setShow(true))
                        .setData(new GaugeDataItem[]{new GaugeDataItem().setValue(50).setName("SCORE")}));

        Jt.echarts(gauge).use();
    }

}
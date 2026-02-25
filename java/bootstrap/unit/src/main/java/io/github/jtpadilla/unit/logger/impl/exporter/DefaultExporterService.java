package io.github.jtpadilla.unit.logger.impl.exporter;

import io.github.jtpadilla.unit.logger.spi.ExporterData;
import io.github.jtpadilla.unit.logger.spi.IUnitLoggerExporter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;

public class DefaultExporterService implements IUnitLoggerExporter {

    @Override
    public void export(ExporterData exporterData) {
        StringBuilder logBuilder = new StringBuilder();

        // Timestamp
        logBuilder.append("[").append(Instant.now().toString()).append("] ");

        // Nivel de log
        logBuilder.append("[").append(exporterData.getLevel().toString()).append("] ");

        // Información de la unidad
        logBuilder.append("[").append(exporterData.getUnitContext().getUnit().getUnitName()).append("] ");

        // Labels adicionales del contexto
        Map<String, String> labels = exporterData.getUnitContext().getLabels();
        if (!labels.isEmpty()) {
            logBuilder.append("[Labels: ");
            labels.forEach((key, value) -> logBuilder.append(key).append("=").append(value).append(" "));
            logBuilder.append("] ");
        }

        // Trace si está disponible
        exporterData.getUnitContext().getTrace().ifPresent(trace -> 
            logBuilder.append("[Trace: ").append(trace).append("] ")
        );

        // Mensaje principal
        logBuilder.append(exporterData.getMsg());

        // Entradas estructuradas adicionales
        if (!exporterData.getStructEntries().isEmpty()) {
            logBuilder.append("\n  Datos estructurados:");
            for (ExporterData.StructEntry entry : exporterData.getStructEntries()) {
                logBuilder.append("\n    - ").append(entry.key())
                         .append(": ").append(entry.value());
            }
        }

        // Excepción si está disponible
        exporterData.getThrowable().ifPresent(throwable -> {
            logBuilder.append("\n  Excepción: ").append(throwable.getClass().getName())
                     .append(": ").append(throwable.getMessage());
            
            // Stack trace completo
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            logBuilder.append("\n  Stack trace:\n").append(sw.toString());
        });

        // Imprimir el log completo
        System.err.println(logBuilder.toString());
    }

    /*
    @Override
    public void export(ExporterData exporterData) {
        System.err.printf(
                "%s %s %s %s%n",
                Instant.now().toString(),
                exporterData.getUnitContext().getUnit().getUnitName(),
                exporterData.getLevel().toString(),
                exporterData.getMsg());
    }
    */

}

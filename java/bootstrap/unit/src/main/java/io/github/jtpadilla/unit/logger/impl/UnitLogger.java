package io.github.jtpadilla.unit.logger.impl;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.logger.UnitLoggerData;
import io.github.jtpadilla.unit.logger.UnitLoggerLevel;
import io.github.jtpadilla.unit.logger.labels.UnitLoggerContext;
import io.github.jtpadilla.unit.logger.spi.ExporterData;
import io.github.jtpadilla.unit.logger.spi.IUnitLoggerExporter;

import java.util.Optional;
import java.util.function.Supplier;

public class UnitLogger implements IUnitLogger {

    final DB db;
    final Entry entry;
    final UnitLoggerContext context;
    final IUnitLoggerExporter exporter;

    public UnitLogger(DB db, Entry entry, UnitLoggerContext context, IUnitLoggerExporter exporter) {
        this.db = db;
        this.entry = entry;
        this.context = context;
        this.exporter = exporter;
    }

    @Override
    public UnitLoggerContext getUnitContext() {
        return context;
    }

    @Override
    public boolean isLevelEnabled(UnitLoggerLevel level) {
        return entry.isLoggable(level);
    }

    @Override
    public UnitLoggerLevel getEfectiveLevel() {
        return entry.getEffectiveLevel();
    }

    @Override
    public Optional<UnitLoggerLevel> getLevel() {
        return entry.getLevel();
    }

    @Override
    public void log(UnitLoggerData unitLoggerData) {
        if (entry.isLoggable(unitLoggerData.getLevel())) {
            exporter.export(ExporterData.newBuilder(context, unitLoggerData).build());
        }
    }

    @Override
    public void log(UnitLoggerLevel level, String msg) {
        if (entry.isLoggable(level)) {
            final ExporterData data = ExporterData.newBuilder(context, level, msg).build();
            exporter.export(data);
        }
    }

    @Override
    public void error(String msg) {
        if (entry.isLoggable(UnitLoggerLevel.ERROR)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.ERROR, msg).build();
            exporter.export(data);
        }
    }

    @Override
    public void warning(String msg) {
        if (entry.isLoggable(UnitLoggerLevel.WARNING)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.WARNING, msg).build();
            exporter.export(data);
        }
    }

    @Override
    public void info(String msg) {
        if (entry.isLoggable(UnitLoggerLevel.INFO)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.INFO, msg).build();
            exporter.export(data);
        }
    }

    @Override
    public void config(String msg) {
        if (entry.isLoggable(UnitLoggerLevel.CONFIG)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.CONFIG, msg).build();
            exporter.export(data);
        }
    }

    @Override
    public void debug(String msg) {
        if (entry.isLoggable(UnitLoggerLevel.DEBUG)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.DEBUG, msg).build();
            exporter.export(data);
        }
    }

    @Override
    public void log(UnitLoggerLevel level, Supplier<String> msgSupplier) {
        if (entry.isLoggable(level)) {
            final ExporterData data = ExporterData.newBuilder(context, level, msgSupplier.get()).build();
            exporter.export(data);
        }
    }

    @Override
    public void error(Supplier<String> msgSupplier) {
        if (entry.isLoggable(UnitLoggerLevel.ERROR)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.ERROR, msgSupplier.get()).build();
            exporter.export(data);
        }
    }

    @Override
    public void warning(Supplier<String> msgSupplier) {
        if (entry.isLoggable(UnitLoggerLevel.WARNING)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.WARNING, msgSupplier.get()).build();
            exporter.export(data);
        }
    }

    @Override
    public void info(Supplier<String> msgSupplier) {
        if (entry.isLoggable(UnitLoggerLevel.INFO)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.INFO, msgSupplier.get()).build();
            exporter.export(data);
        }
    }

    @Override
    public void config(Supplier<String> msgSupplier) {
        if (entry.isLoggable(UnitLoggerLevel.CONFIG)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.CONFIG, msgSupplier.get()).build();
            exporter.export(data);
        }
    }

    @Override
    public void debug(Supplier<String> msgSupplier) {
        if (entry.isLoggable(UnitLoggerLevel.DEBUG)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.DEBUG, msgSupplier.get()).build();
            exporter.export(data);
        }
    }

    @Override
    public void log(UnitLoggerLevel level, String msg, Throwable throwable) {
        if (!entry.isLoggable(level)) {
            final ExporterData data = ExporterData.newBuilder(context, level, msg)
                    .setThrowable(throwable)
                    .build();
            exporter.export(data);
        }
    }

    @Override
    public void error(String msg, Throwable throwable) {
        if (entry.isLoggable(UnitLoggerLevel.ERROR)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.ERROR, msg)
                    .setThrowable(throwable)
                    .build();
            exporter.export(data);
        }
    }

    @Override
    public void warning(String msg, Throwable throwable) {
        if (entry.isLoggable(UnitLoggerLevel.WARNING)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.WARNING, msg)
                    .setThrowable(throwable)
                    .build();
            exporter.export(data);
        }
    }

    @Override
    public void info(String msg, Throwable throwable) {
        if (entry.isLoggable(UnitLoggerLevel.INFO)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.INFO, msg)
                    .setThrowable(throwable)
                    .build();
            exporter.export(data);
        }
    }

    @Override
    public void config(String msg, Throwable throwable) {
        if (entry.isLoggable(UnitLoggerLevel.CONFIG)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.CONFIG, msg)
                    .setThrowable(throwable)
                    .build();
            exporter.export(data);
        }
    }

    @Override
    public void debug(String msg, Throwable throwable) {
        if (entry.isLoggable(UnitLoggerLevel.DEBUG)) {
            final ExporterData data = ExporterData.newBuilder(context, UnitLoggerLevel.DEBUG, msg)
                    .setThrowable(throwable)
                    .build();
            exporter.export(data);
        }
    }

}

package io.github.jtpadilla.unit.logger.spi;

@FunctionalInterface
public interface IUnitLoggerExporter {
    void export(ExporterData exporterData);
}

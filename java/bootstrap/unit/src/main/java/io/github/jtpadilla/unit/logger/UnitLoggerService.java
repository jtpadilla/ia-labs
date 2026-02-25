package io.github.jtpadilla.unit.logger;

import io.github.jtpadilla.unit.Unit;
import io.github.jtpadilla.unit.logger.config.UnitLoggerCustomLevelConfig;
import io.github.jtpadilla.unit.logger.config.UnitLoggerRootLevelConfig;
import io.github.jtpadilla.unit.logger.impl.DB;
import io.github.jtpadilla.unit.logger.impl.Entry;
import io.github.jtpadilla.unit.logger.impl.UnitLogger;
import io.github.jtpadilla.unit.logger.impl.exporter.DefaultExporterService;
import io.github.jtpadilla.unit.logger.labels.UnitLoggerContext;
import io.github.jtpadilla.unit.logger.spi.ExporterData;
import io.github.jtpadilla.unit.logger.spi.IUnitLoggerExporter;

import java.util.Optional;
import java.util.Set;

public class UnitLoggerService {

    static final private DB loggerDB;
    static final private IUnitLoggerExporter exporterProxy;

    static private volatile IUnitLoggerExporter exporterDynamic;

    static {
        loggerDB = new DB();
        exporterDynamic = new DefaultExporterService();
        exporterProxy = new ExporterProxy();

        UnitLoggerRootLevelConfig.config(UnitLoggerService::setLevel);
        UnitLoggerCustomLevelConfig.config(UnitLoggerService::setLevel);
    }

    static public void setExporter(IUnitLoggerExporter exporter) {
        exporterDynamic = exporter;
    }

    static public void setLevel(String name, UnitLoggerLevel level) {
        loggerDB.setLevel(name, level);
    }

    static public Optional<UnitLoggerLevel> getLevel(String name) {
        return loggerDB.getLogger(name).getLevel();
    }

    static public UnitLoggerLevel getEfectiveLevel(String name) {
        return loggerDB.getEffectiveLevel(name);
    }

    static public Set<String> getLoggerNames() {
        return loggerDB.getLoggerNames();
    }

    static public IUnitLogger getLogger(String name) {
        final Unit unit = new Unit(name);
        final UnitLoggerContext unitLoggerContext = UnitLoggerContext.newBuilder(unit).build();
        return getLogger(unitLoggerContext);
    }

    static public IUnitLogger getLogger(UnitLoggerContext unitLoggerContext) {
        final Entry entry = loggerDB.getLogger(unitLoggerContext.getUnit().getUnitName());
        return new UnitLogger(loggerDB, entry, unitLoggerContext, exporterProxy);
    }

    private static class ExporterProxy implements IUnitLoggerExporter {

        @Override
        public void export(ExporterData exporterData) {
            final IUnitLoggerExporter currentExporter = exporterDynamic;
            currentExporter.export(exporterData);
        }

    }

}

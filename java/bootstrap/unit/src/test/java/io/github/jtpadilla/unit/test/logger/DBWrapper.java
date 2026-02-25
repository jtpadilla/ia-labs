package io.github.jtpadilla.unit.test.logger;

import io.github.jtpadilla.unit.Unit;
import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.logger.UnitLoggerLevel;
import io.github.jtpadilla.unit.logger.impl.DB;
import io.github.jtpadilla.unit.logger.impl.Entry;
import io.github.jtpadilla.unit.logger.impl.UnitLogger;
import io.github.jtpadilla.unit.logger.labels.UnitLoggerContext;
import io.github.jtpadilla.unit.logger.spi.ExporterData;
import io.github.jtpadilla.unit.logger.spi.IUnitLoggerExporter;

import java.util.Optional;
import java.util.Set;

public class DBWrapper {

    final private DB db;

    public DBWrapper() {
        this.db = new DB();
    }

    public void setLevel(String name, UnitLoggerLevel level) {
        db.setLevel(name, level);
    }

    public Optional<UnitLoggerLevel> getLevel(String name) {
        return db.getLogger(name).getLevel();
    }

    public UnitLoggerLevel getEfectiveLevel(String name) {
        return db.getEffectiveLevel(name);
    }

    public Set<String> getLoggerNames() {
        return db.getLoggerNames();
    }

    public IUnitLogger getLogger(String name) {
        UnitLoggerContext context = UnitLoggerContext.newBuilder(new Unit(name)).build();
        final Entry entry = db.getLogger(name);
        return new UnitLogger(db, entry, context,  exporterProxy);
    }

    static private IUnitLoggerExporter exporterProxy = new IUnitLoggerExporter() {

        @Override
        public void export(ExporterData exporterData) {

        }

    };

}

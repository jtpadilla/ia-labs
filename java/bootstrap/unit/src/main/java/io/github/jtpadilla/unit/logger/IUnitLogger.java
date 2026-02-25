package io.github.jtpadilla.unit.logger;

import io.github.jtpadilla.unit.logger.labels.UnitLoggerContext;

import java.util.Optional;
import java.util.function.Supplier;

public interface IUnitLogger {

    UnitLoggerContext getUnitContext();

    boolean isLevelEnabled(UnitLoggerLevel level);

    Optional<UnitLoggerLevel> getLevel();
    UnitLoggerLevel getEfectiveLevel();

    // Nativo
    void log(UnitLoggerData data);

    // Clasicoa
    void log(UnitLoggerLevel level, String msg);
    void error(String msg);
    void warning(String msg);
    void info(String msg);
    void config(String msg);
    void debug(String msg);

    void log(UnitLoggerLevel level, Supplier<String> msgSupplier);
    void error(Supplier<String> msgSupplier);
    void warning(Supplier<String> msgSupplier);
    void info(Supplier<String> msgSupplier);
    void config(Supplier<String> msgSupplier);
    void debug(Supplier<String> msgSupplier);

    void log(UnitLoggerLevel level, String msg, Throwable throwable);
    void error(String msg, Throwable throwable);
    void warning(String msg, Throwable throwable);
    void info(String msg, Throwable throwable);
    void config(String msg, Throwable throwable);
    void debug(String msg, Throwable throwable);

}

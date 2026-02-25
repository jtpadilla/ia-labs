package io.github.jtpadilla.unit.logger;

import java.util.Optional;

public enum UnitLoggerLevel {
    OFF(Integer.MAX_VALUE),
    ERROR(1000),
    WARNING(900),
    INFO(700),
    CONFIG(600),
    DEBUG(500),
    ALL(Integer.MIN_VALUE);

    final int value;

    UnitLoggerLevel(int value) {
        this.value = value;
    }

    public String getName() {
        return this.name();
    }

    public boolean isLoggable(UnitLoggerLevel other) {
        return other.value >= this.value;
    }

    public static Optional<UnitLoggerLevel> parse(String name) {
        try {
            return Optional.of(valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}

package io.github.jtpadilla.unit.logger.impl;

import io.github.jtpadilla.unit.logger.UnitLoggerLevel;

public class RootEntry extends Entry {

    public RootEntry() {
        super("");
        this.level = UnitLoggerLevel.INFO;
    }

}

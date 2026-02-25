package io.github.jtpadilla.unit.parameter;

import io.github.jtpadilla.unit.Unit;
import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.logger.UnitLoggerService;
import io.github.jtpadilla.unit.logger.labels.UnitLoggerContext;

public class UnitResource {
    static public final Unit UNIT = new Unit("unit.parameter");
    static public final UnitLoggerContext CONTEXT = UnitLoggerContext.newBuilder(UNIT).build();
    static public final IUnitLogger LOGGER = UnitLoggerService.getLogger(CONTEXT);
}

package io.github.jtpadilla.unit.test.logger;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.logger.UnitLoggerLevel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceNonEffectiveTests {

    private DBWrapper loggerService;

    @Before
    public void initLoggerService() {
        loggerService = new DBWrapper();
    }

    @Test
    public void customFirstInheritanceLevelTest() {

        loggerService.setLevel("", UnitLoggerLevel.WARNING);
        loggerService.setLevel("a.b", UnitLoggerLevel.INFO);
        loggerService.setLevel("a.b.c.d", UnitLoggerLevel.DEBUG);

        // Root
        IUnitLogger loggerRoot = loggerService.getLogger("");
        assertEquals(UnitLoggerLevel.WARNING, loggerRoot.getEfectiveLevel());
        assertTrue(loggerRoot.getLevel().isPresent());
        assertEquals(UnitLoggerLevel.WARNING, loggerRoot.getLevel().get());

        // a
        IUnitLogger loggerA = loggerService.getLogger("a");
        assertEquals(UnitLoggerLevel.WARNING, loggerA.getEfectiveLevel());
        assertFalse(loggerA.getLevel().isPresent());

        // a.b
        IUnitLogger loggerB = loggerService.getLogger("a.b");
        assertEquals(UnitLoggerLevel.INFO, loggerB.getEfectiveLevel());
        assertTrue(loggerB.getLevel().isPresent());
        assertEquals(UnitLoggerLevel.INFO, loggerB.getLevel().get());

        // a.b.c
        IUnitLogger loggerC = loggerService.getLogger("a.b.c");
        assertEquals(UnitLoggerLevel.INFO, loggerC.getEfectiveLevel());
        assertFalse(loggerC.getLevel().isPresent());

        // a.b.c.d
        IUnitLogger loggerD = loggerService.getLogger("a.b.c.d");
        assertEquals(UnitLoggerLevel.DEBUG, loggerD.getEfectiveLevel());
        assertTrue(loggerD.getLevel().isPresent());
        assertEquals(UnitLoggerLevel.DEBUG, loggerD.getLevel().get());

    }

}

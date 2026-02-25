package io.github.jtpadilla.unit.test.logger;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.logger.UnitLoggerLevel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceEfectiveTests {

    private DBWrapper loggerService;

    @Before
    public void initLoggerService() {
        loggerService = new DBWrapper();
    }

    @Test
    public void customFirstInheritanceLevelTest() {

        IUnitLogger loggerRoot = loggerService.getLogger("");

        IUnitLogger loggerA = loggerService.getLogger("a");
        loggerService.setLevel("a", UnitLoggerLevel.CONFIG);

        IUnitLogger loggerB = loggerService.getLogger("a.b");
        loggerService.setLevel("a.b", UnitLoggerLevel.DEBUG);

        // root
        assertTrue(loggerRoot.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(loggerRoot.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(loggerRoot.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(loggerRoot.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(loggerRoot.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(loggerRoot.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(loggerRoot.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(loggerRoot.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(loggerRoot.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(loggerRoot.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertEquals(UnitLoggerLevel.INFO, loggerRoot.getEfectiveLevel());

        // loggerA
        assertTrue(loggerA.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(loggerA.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(loggerA.isLevelEnabled(UnitLoggerLevel.INFO));
        assertTrue(loggerA.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(loggerA.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(loggerA.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(loggerA.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(loggerA.isLevelEnabled(UnitLoggerLevel.INFO));
        assertTrue(loggerA.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(loggerA.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertEquals(UnitLoggerLevel.CONFIG, loggerA.getEfectiveLevel());

        // loggerB
        assertTrue(loggerB.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(loggerB.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(loggerB.isLevelEnabled(UnitLoggerLevel.INFO));
        assertTrue(loggerB.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertTrue(loggerB.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(loggerB.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(loggerB.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(loggerB.isLevelEnabled(UnitLoggerLevel.INFO));
        assertTrue(loggerB.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertTrue(loggerB.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertEquals(UnitLoggerLevel.DEBUG, loggerB.getEfectiveLevel());

    }

}

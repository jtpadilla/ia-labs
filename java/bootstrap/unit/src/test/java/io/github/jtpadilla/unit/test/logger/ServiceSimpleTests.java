package io.github.jtpadilla.unit.test.logger;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.logger.UnitLoggerLevel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceSimpleTests {

    private DBWrapper loggerService;

    @Before
    public void initLoggerService() {
        loggerService = new DBWrapper();
    }

    @Test
    public void defaultRootLevelTest() {
        IUnitLogger logger = loggerService.getLogger("");
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(logger.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(logger.isLevelEnabled(UnitLoggerLevel.DEBUG));
    }

    @Test
    public void defaultFirstLevelTest() {
        IUnitLogger logger = loggerService.getLogger("a");
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(logger.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(logger.isLevelEnabled(UnitLoggerLevel.DEBUG));
    }

    @Test
    public void defaultSecondLevelTest() {
        IUnitLogger logger = loggerService.getLogger("a.b");
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(logger.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(logger.isLevelEnabled(UnitLoggerLevel.DEBUG));
    }

    @Test
    public void customRootLevelTest() {
        IUnitLogger logger = loggerService.getLogger("");
        loggerService.setLevel("", UnitLoggerLevel.CONFIG);
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.INFO));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(logger.isLevelEnabled(UnitLoggerLevel.DEBUG));
    }

    @Test
    public void customFirstLevelTest() {
        IUnitLogger logger = loggerService.getLogger("a");
        loggerService.setLevel("", UnitLoggerLevel.CONFIG);
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.INFO));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(logger.isLevelEnabled(UnitLoggerLevel.DEBUG));
    }

    @Test
    public void customSecondLevelTest() {
        IUnitLogger logger = loggerService.getLogger("a.b");
        loggerService.setLevel("", UnitLoggerLevel.CONFIG);
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.INFO));
        assertTrue(logger.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(logger.isLevelEnabled(UnitLoggerLevel.DEBUG));
    }

}

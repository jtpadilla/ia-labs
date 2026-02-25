package io.github.jtpadilla.unit.test.logger;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.logger.UnitLoggerLevel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceEnabledLevelTests {

    private DBWrapper loggerService;

    @Before
    public void initLoggerService() {
        loggerService = new DBWrapper();
    }

    @Test
    public void defaultRootLevelTest() {
        IUnitLogger root = loggerService.getLogger("");
        IUnitLogger childA = loggerService.getLogger("a");
        IUnitLogger childB = loggerService.getLogger("a.b");
        IUnitLogger childC = loggerService.getLogger("a.b.c");

        assertTrue(root.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(root.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(root.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(root.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(root.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(childA.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(childA.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(childA.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(childA.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(childA.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(childB.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(childB.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(childB.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(childB.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(childB.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(childC.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(childC.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertTrue(childC.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(childC.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(childC.isLevelEnabled(UnitLoggerLevel.DEBUG));

    }

    @Test
    public void changeRootLevelTest() {
        IUnitLogger root = loggerService.getLogger("");
        IUnitLogger childA = loggerService.getLogger("a");
        IUnitLogger childB = loggerService.getLogger("a.b");
        IUnitLogger childC = loggerService.getLogger("a.b.c");

        loggerService.setLevel("", UnitLoggerLevel.WARNING);

        assertTrue(root.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(root.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertFalse(root.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(root.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(root.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(childA.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(childA.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertFalse(childA.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(childA.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(childA.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(childB.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(childB.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertFalse(childB.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(childB.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(childB.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(childC.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(childC.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertFalse(childC.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(childC.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(childC.isLevelEnabled(UnitLoggerLevel.DEBUG));

    }

    @Test
    public void changeBLevelTest() {
        IUnitLogger root = loggerService.getLogger("");
        IUnitLogger childA = loggerService.getLogger("a");
        IUnitLogger childB = loggerService.getLogger("a.b");
        IUnitLogger childC = loggerService.getLogger("a.b.c");

        loggerService.setLevel("", UnitLoggerLevel.WARNING);
        loggerService.setLevel("a.b", UnitLoggerLevel.ERROR);

        assertTrue(root.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(root.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertFalse(root.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(root.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(root.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(childA.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertTrue(childA.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertFalse(childA.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(childA.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(childA.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(childB.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertFalse(childB.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertFalse(childB.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(childB.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(childB.isLevelEnabled(UnitLoggerLevel.DEBUG));

        assertTrue(childC.isLevelEnabled(UnitLoggerLevel.ERROR));
        assertFalse(childC.isLevelEnabled(UnitLoggerLevel.WARNING));
        assertFalse(childC.isLevelEnabled(UnitLoggerLevel.INFO));
        assertFalse(childC.isLevelEnabled(UnitLoggerLevel.CONFIG));
        assertFalse(childC.isLevelEnabled(UnitLoggerLevel.DEBUG));

    }
}

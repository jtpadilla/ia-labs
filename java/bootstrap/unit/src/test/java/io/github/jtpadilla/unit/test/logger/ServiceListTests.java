package io.github.jtpadilla.unit.test.logger;

import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.logger.UnitLoggerLevel;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class ServiceListTests {

    record DataTest(String name, UnitLoggerLevel set, UnitLoggerLevel expected) {}
    static List<DataTest> data;

    private DBWrapper loggerService;

    @Before
    public void initLoggerService() {
        loggerService = new DBWrapper();
        List<DataTest> inmutable = List.of(
                new DataTest("", UnitLoggerLevel.WARNING, UnitLoggerLevel.WARNING),
                new DataTest("a", null, UnitLoggerLevel.WARNING),
                new DataTest("a.b", UnitLoggerLevel.INFO, UnitLoggerLevel.INFO),
                new DataTest("a.b.c", null, UnitLoggerLevel.INFO),
                new DataTest("a.b.c,d", UnitLoggerLevel.DEBUG, UnitLoggerLevel.DEBUG)
        );
        data = new ArrayList<>(inmutable);
    }

    Optional<DataTest> consume(String name) {
        Optional<DataTest> found = data.stream()
                .filter(dataTest -> dataTest.name.equals(name))
                .findFirst();
        if (found.isPresent()) {
            data.remove(found.get());
            return found;
        } else {
            return Optional.empty();
        }
    }

    void fromUnitLogger(IUnitLogger logger, DataTest dataTest) {
        assertEquals(logger.getEfectiveLevel(), dataTest.expected);
        if (dataTest.set != null) {
            Optional<UnitLoggerLevel> level = logger.getLevel();
            assertTrue(level.isPresent());
            assertEquals(dataTest.set, level.get());
        } else {
            assertFalse(logger.getLevel().isPresent());
        }
    }

    @Test
    public void customFirstInheritanceLevelTest() {

        // Se inicializa
        for (DataTest dataTest : data) {
            if (dataTest.set == null) {
                // Forzamos la creacion sin asignarle explicitamente nivel
                loggerService.getLogger(dataTest.name);
            } else {
                // Se cre con un nivel explicito
                loggerService.setLevel(dataTest.name, dataTest.set);
            }
        }

        // Para cada nombre que consta en la lista...
        for (String name  : loggerService.getLoggerNames()) {

            // Se consume el correspondiente inicializacion
            Optional<DataTest> consumed = consume(name);
            assertTrue(consumed.isPresent());

            // Se obtiene la entrada de logger
            IUnitLogger unitLogger = loggerService.getLogger(name);

            // Se comparan
            fromUnitLogger(unitLogger, consumed.get());

        }
        assertTrue(data.isEmpty());

    }

}

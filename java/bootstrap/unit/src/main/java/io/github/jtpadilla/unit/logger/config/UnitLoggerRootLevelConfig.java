package io.github.jtpadilla.unit.logger.config;

import io.github.jtpadilla.unit.logger.UnitLoggerLevel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

// Esta propiedad permite especificar el nivel del Logger de ROOT.
// Si no se utiliza esta variable se utilizara el valor por defecto
//
// java_binary(
//      ...
//      name = "name",
//      srcs = glob(["src/main/java/**/*.java"]),
//      jvm_flags = [
//           "'-DSC_UNIT_LOGGER_ROOT_LEVEL=WARNING'",
//      ],
//      ...
// )
public class UnitLoggerRootLevelConfig {

    final static private Logger LOGGER = Logger.getLogger("bootstrap.unit.logger.rootconfig");

    static final private String SC_UNIT_LOGGER_ROOT_LEVEL = "SC_UNIT_LOGGER_ROOT_LEVEL";

    static public void config(BiConsumer<String, UnitLoggerLevel> biConsumer) {

        // Se carga desde el SO la variable con la definicio del nivel para ROOT
        final String rootLevelStr = System.getProperty(SC_UNIT_LOGGER_ROOT_LEVEL);

        // Si no esta definida se utiliza el valor por defecto
        if (rootLevelStr == null) {
            LOGGER.config("No se ha definido un nivel personalizado para ROOT (se utilizara el valor por defecto)");
            return;
        }

        // Si esta definida asi que se intenta parsear
        final Optional<UnitLoggerLevel> rootLevelOptional = UnitLoggerLevel.parse(rootLevelStr);
        if (rootLevelOptional.isEmpty()) {
            LOGGER.warning("La definicion del nivel personalizado para ROOT es invalida (se utilizara el valor por defecto)");
            return;
        }

        // Tenemos un nivel valido asi que se establece la configuracion
        final UnitLoggerLevel rootLevel = rootLevelOptional.get();
        LOGGER.warning(String.format("La definicion del nivel para ROOT se ha configurado a '%s'", rootLevel));
        biConsumer.accept("", rootLevel);

    }

}

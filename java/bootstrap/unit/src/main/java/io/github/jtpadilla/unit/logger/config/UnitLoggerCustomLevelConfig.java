package io.github.jtpadilla.unit.logger.config;

import io.github.jtpadilla.unit.logger.UnitLoggerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

// Estas propiedades permiten construir una tabla de entradas de logger
// especificando para cada una de ellas las clave del LOG y su nivel.
//
// java_binary(
//      ...
//      name = "name",
//      srcs = glob(["src/main/java/**/*.java"]),
//      jvm_flags = [
//           "'-DSC_UNIT_LOGGER_CUSTOM_LEVEL=pkg1.pkg2.one:DEBUG",
//           "'-DSC_UNIT_LOGGER_CUSTOM_LEVEL_1=pkg1.pkg2.two:DEBUG'",
//           "'-DSC_UNIT_LOGGER_CUSTOM_LEVEL_2=pkg1.pkg2.three:INFO'",
//           "'-DSC_UNIT_LOGGER_CUSTOM_LEVEL_3=pkg1.pkg3.four:DEBUG;pkg1.pkg4.five:INFO'",
//      ],
//      ...
// )
public class UnitLoggerCustomLevelConfig {

    final static private Logger LOGGER = Logger.getLogger("bootstrap.unit.logger.customconfig");

    static final private List<String> SC_UNIT_LOGGER_CUSTOM_LEVEL_LIST = List.of(
            "SC_UNIT_LOGGER_CUSTOM_LEVEL",
            "SC_UNIT_LOGGER_CUSTOM_LEVEL_0",
            "SC_UNIT_LOGGER_CUSTOM_LEVEL_1",
            "SC_UNIT_LOGGER_CUSTOM_LEVEL_2",
            "SC_UNIT_LOGGER_CUSTOM_LEVEL_3",
            "SC_UNIT_LOGGER_CUSTOM_LEVEL_4",
            "SC_UNIT_LOGGER_CUSTOM_LEVEL_5",
            "SC_UNIT_LOGGER_CUSTOM_LEVEL_6",
            "SC_UNIT_LOGGER_CUSTOM_LEVEL_7",
            "SC_UNIT_LOGGER_CUSTOM_LEVEL_8",
            "SC_UNIT_LOGGER_CUSTOM_LEVEL_9"
    );

    private record CustomLevelEntry(String name, UnitLoggerLevel level) {}

    static public void config(BiConsumer<String, UnitLoggerLevel> biConsumer) {

        // Para depositar los resultados
        final List<CustomLevelEntry> customEntries = new ArrayList<>();

        // Cada una de las propiedades puede tener varios entradas separadas por un ";" asi
        // que se acumulan todas en la lista global.
        SC_UNIT_LOGGER_CUSTOM_LEVEL_LIST.forEach(paraneterName-> customEntries.addAll(getCustomLevelSublist(paraneterName)));

        // Se asignan los niveles CUSTOM
        customEntries.forEach(customEntry->biConsumer.accept(customEntry.name(), customEntry.level()));

        // Se muestra la lista de niveles CUSTOM
        if (customEntries.isEmpty()) {
            LOGGER.warning("No hay configuraciones extra para los loggers");
        } else {
            customEntries.forEach(
                    customEntry-> LOGGER.warning(
                            String.format(
                                    "El logger '%s' se configura con el nivel '%s'",
                                    customEntry.name(),
                                    customEntry.level().getName()
                            )
                    )
            );
        }
    }

    static private List<CustomLevelEntry> getCustomLevelSublist(String parameterName) {
        final String parameterValue = System.getProperty(parameterName);
        if (parameterValue != null) {
            return Stream.of(parameterValue.split(";"))
                    .map(UnitLoggerCustomLevelConfig::parseCustomEntry)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        } else {
            return Collections.emptyList();
        }
    }

    static private Optional<CustomLevelEntry> parseCustomEntry(String segment) {

        final String[] entryString = segment.split(":");

        if (entryString.length == 2 && !entryString[0].trim().isEmpty() && !entryString[1].trim().isEmpty()) {

            // El primer campo es el nombre del logger
            final String loggerName = entryString[0].trim();

            // El segundo campo es el Nivel
            Optional<UnitLoggerLevel> levelOptional = UnitLoggerLevel.parse(entryString[1].trim());
            if (levelOptional.isPresent()) {
                return Optional.of(new CustomLevelEntry(loggerName, levelOptional.get()));
            } else {
                LOGGER.severe(String.format("Hay un segmento mal formado \"%s\"", segment));
                return Optional.empty();
            }

        } else {
            LOGGER.severe(String.format("Hay un segmento mal formado \"%s\"", segment));
            return Optional.empty();
        }

    }

}

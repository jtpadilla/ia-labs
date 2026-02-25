package io.github.jtpadilla.unit;

import java.util.logging.Logger;

public class UnitEnviromentConfig {

    final static private Logger LOGGER = Logger.getLogger("bootstrap.unit.enviromentconfig");

    static final private String UNIT_ENVIROMENT_KEY = "UNIT_ENVIROMENT";
    static final private String UNIT_ENVIROMENT_DEF_VALUE = "dev";

    static public String getConfig() {

        // Se carga desde el SO la variable con la definicio del nivel para ROOT
        final String enviromentStr = System.getProperty(UNIT_ENVIROMENT_KEY);

        // Si no esta definida se utiliza el valor por defecto
        if (enviromentStr == null) {
            LOGGER.config("No se ha definido un nivel personalizado para ROOT (se utilizara el valor por defecto)");
            return UNIT_ENVIROMENT_DEF_VALUE;
        } else {
            return enviromentStr;
        }

    }

}

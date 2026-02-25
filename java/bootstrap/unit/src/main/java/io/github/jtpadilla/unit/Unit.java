package io.github.jtpadilla.unit;

import java.util.Objects;
import java.util.logging.Logger;

/// Esta clase identifica de forma inequivoca el codigo de una libreria.
public class Unit {

    final static private Logger LOGGER = Logger.getLogger(Unit.class.getName());

    final static private String enviroment;
    volatile static private String component;

    static {

        component = "default";
        LOGGER.info(String.format("Componente inicializado: %s", component));

        enviroment = UnitEnviromentConfig.getConfig();
        LOGGER.info(String.format("Entorno inicializado: %s", enviroment));

    }

    static public void setComponent(String component) {
        Unit.component = Objects.requireNonNull(component, "El nombre proporcionado para inicializar el componente no esta informado.");
        LOGGER.info(String.format("Componente actualizado: %s", component));
    }

    final private String unitName;

    public Unit(String unitName) {
        this.unitName = Objects.requireNonNull(unitName, "El nombre del unit no esta informado.");
    }

    public String getEnviroment() {
        return Unit.enviroment;
    }

    public String getComponentName() {
        return Unit.component;
    }

    public String getUnitName() {
        return unitName;
    }

    @Override
    public String toString() {
        return "Unit{" +
                "unitName='" + unitName + '\'' +
                '}';
    }

}

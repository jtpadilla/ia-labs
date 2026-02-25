package io.github.jtpadilla.unit.parameter.driver;

import io.github.jtpadilla.unit.parameter.IParameterDriver;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class DriverSelector {

    static final private String SP_PARAMETERS_DRIVER = "SC_PARAMETERS_DRIVER";

    @Nullable
    static volatile private IParameterDriver currentDriver = null;

    static public IParameterDriver getDriver() {
        synchronized (DriverSelector.class) {
            if (currentDriver == null) {
                currentDriver = createDriver();
            }
        }
        return currentDriver;
    }

    static private IParameterDriver createDriver() {

        final Optional<String> parameterDriver = Optional.ofNullable(System.getProperty(SP_PARAMETERS_DRIVER));

        if (parameterDriver.isEmpty()) {
            return new ResourceDriver("parameters.txt");
        }

        if (parameterDriver.get().equalsIgnoreCase("resource")) {
            return new ResourceDriver("parameters.txt");
        }

        if (parameterDriver.get().toLowerCase().startsWith("resource:")) {
            return new ResourceDriver(parameterDriver.get().substring("resource:".length()));
        }

        if (parameterDriver.get().equalsIgnoreCase("enviroment")) {
            return new EnviromentDriver();
        }

        if (parameterDriver.get().toLowerCase().startsWith("properties:")) {
            return new PropertiesDriver(parameterDriver.get().substring("properties:".length()));
        }

        throw new IllegalArgumentException(String.format("El valor de la variable %s no es valido.", SP_PARAMETERS_DRIVER));

    }

}

package io.github.jtpadilla.unit.parameter.driver;

import io.github.jtpadilla.unit.parameter.IParameterDriver;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import static io.github.jtpadilla.unit.parameter.UnitResource.LOGGER;

public class PropertiesDriver implements IParameterDriver {

    final private Properties props = new Properties();

    PropertiesDriver(String resourcePath) {
        try (FileInputStream fis = new FileInputStream(resourcePath)) {
            props.load(fis);
        } catch (IOException e) {
            final RuntimeException runtimeEx = new RuntimeException(String.format("Imposible abrir el fichero '%s'", resourcePath), e);
            LOGGER.error(String.format("No se ha podido abrir el fichero de propiedades'%s'", resourcePath), runtimeEx);
            throw runtimeEx;
        }
    }

    @Override
    public Optional<String> resolveParameterName(String name) {
        return Optional.ofNullable(props.getProperty(name));
    }

}

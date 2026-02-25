package io.github.jtpadilla.unit.parameter.driver;

import io.github.jtpadilla.unit.parameter.IParameterDriver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import static io.github.jtpadilla.unit.parameter.UnitResource.LOGGER;

public class ResourceDriver implements IParameterDriver {

    final private Properties props = new Properties();

    public ResourceDriver(String resourcePath) {
        final InputStream inputStream;
        try {
            inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new RuntimeException(String.format("No se ha encontrado el recurso '%s'", resourcePath));
            }
            props.load(inputStream);
        } catch (IOException e) {
            final RuntimeException runtimeEx = new RuntimeException(String.format("Imposible abrir el recurso '%s'", resourcePath), e);
            LOGGER.error(String.format("No se ha podido abrir el recurso de propiedades'%s'", resourcePath), runtimeEx);
            throw runtimeEx;
        }

    }

    @Override
    public Optional<String> resolveParameterName(String name) {
        return Optional.ofNullable(props.getProperty(name));
    }

}

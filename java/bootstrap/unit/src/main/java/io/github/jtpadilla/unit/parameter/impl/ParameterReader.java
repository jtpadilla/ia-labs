package io.github.jtpadilla.unit.parameter.impl;

import io.github.jtpadilla.unit.Unit;
import io.github.jtpadilla.unit.parameter.IParameterDescriptor;
import io.github.jtpadilla.unit.parameter.IParameterReader;
import io.github.jtpadilla.unit.parameter.IParameterValue;

import java.util.List;
import java.util.Optional;

import static io.github.jtpadilla.unit.parameter.UnitResource.LOGGER;

public class ParameterReader implements IParameterReader {

    static final private String DOUBLE_SEPARATOR = "==" + "=".repeat(90);
    static final private String SIMPLE_SEPARATOR = "= " + "-".repeat(90);

    final private List<ParameterReaderEntry> parameters;

    ParameterReader(Unit unit, List<ParameterReaderEntry> parameters) {

        this.parameters = parameters;

        /////////////////////////////////////////////////////////////////////////////////////////////
        // Se muestran los parametros y sus valores
        /////////////////////////////////////////////////////////////////////////////////////////////

        // Cabecera
        LOGGER.info(DOUBLE_SEPARATOR);
        LOGGER.info(String.format("= Componente=%s", unit.getUnitName()));

        // Separador entre cabecera y descriptores
        LOGGER.info(SIMPLE_SEPARATOR);
        for (int i = 0; i < parameters.size(); i++) {
            parameters.get(i).getDescriptor().printInfo(LOGGER);
            if (i != parameters.size() - 1) {
                LOGGER.info("=");
            }
        }

        // Separacion entre descriptores y valores
        LOGGER.info(SIMPLE_SEPARATOR);

        // Valores de los parametros
        parameters.forEach(e->{
            IParameterValue value = e.getValue();
            IParameterDescriptor descriptor = e.getDescriptor();
            LOGGER.info(String.format("= %s -> %s", descriptor.getName(), value.toString()));
        });

        // Separador del pie
        //LOGGER.info(DOUBLE_SEPARATOR);

        /////////////////////////////////////////////////////////////////////////////////////////////
        // Si hay algun error en los parametros no se puede continuar
        /////////////////////////////////////////////////////////////////////////////////////////////

        // Si hay algun error no se puede continuar
        if (parameters.stream().anyMatch(v->v.getValue().hasError())) {
            throw new IllegalStateException(String.format("El componente '%s' no ha obtenido satisfactoriamente los parametros requeridos", unit.getUnitName()));
        } else {
            //LOGGER.info(String.format("El componente '%s' ha obtenido satisfactoriamente los parametros requeridos.", unit.getUnitName()));
        }

    }

    @Override
    public IParameterValue getValue(IParameterDescriptor parameterDescriptor) {

        Optional<ParameterReaderEntry> entry = parameters.stream()
                .filter(e->e.getDescriptor().getName().equals(parameterDescriptor.getName()))
                .findFirst();

        if (entry.isEmpty()) {
            throw new IllegalArgumentException(String.format("Se ha requerido el parametro %s el cual no esta registrado.", parameterDescriptor.getName()));
        } else {
            return entry.get().getValue();
        }

    }

}

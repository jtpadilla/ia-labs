package io.github.jtpadilla.example.genai.study.simplefunctioncalling.function.impl;

import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.Map;
import java.util.Optional;

record Parameters(double length, double width) {

    static public Parameters create(Map<String, Object> args) throws FunctionGatewayException {
        return new Parameters(
                getLength(args),
                getWidth(args)
        );
    }

    static private double getLength(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> lengthObject = Optional.ofNullable(args.get("length"));
        if (lengthObject.isEmpty()) {
            throw new FunctionGatewayException("Parameter CalculateRentangle.Parameters.lenght in empty");
        }
        if (lengthObject.get() instanceof Number) {
            return ((Number) lengthObject.get()).doubleValue();
        }
        throw new FunctionGatewayException("Parameter CalculateRentangle.Parameters.lenght not is Number");
    }

    static private double getWidth(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> widthObject = Optional.ofNullable(args.get("width"));
        if (widthObject.isEmpty()) {
            throw new FunctionGatewayException("Parameter CalculateRentangle.Parameters.width in empty");
        }
        if (widthObject.get() instanceof Number) {
            return ((Number) widthObject.get()).doubleValue();
        }
        throw new FunctionGatewayException("Parameter CalculateRentangle.Parameters.width not is Number");
    }

}

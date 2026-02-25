package io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.mathcalculator;

import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Parameters {

    static public Parameters create(Map<String, Object> args) throws FunctionGatewayException {
        final Builder builder = builder(createOperation(args), createNumber(args));
        createSecondNumber(args).ifPresent(builder::setSecondNumber);
        builder.addNumbers(createNumbers(args));
        return builder.build();
    }

    static private String createOperation(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("operation"));
        if (object.isEmpty()) {
            throw new FunctionGatewayException("MathCalculatorFunction.Parameter.operation is null");
        }
        if (object.get() instanceof String) {
            return (String) object.get();
        } else {
            throw new FunctionGatewayException("MathCalculatorFunction.Parameter.operation not is String");
        }
    }

    static private double createNumber(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("number"));
        if (object.isEmpty()) {
            throw new FunctionGatewayException("MathCalculatorFunction.Parameter.number is null");
        }
        if (object.get() instanceof Number) {
            return ((Number) object.get()).doubleValue();
        } else {
            throw new FunctionGatewayException("MathCalculatorFunction.Parameter.number not is Number");
        }
    }

    static private Optional<Double> createSecondNumber(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("second_number"));
        if (object.isEmpty()) {
            return Optional.empty();
        }
        if (object.get() instanceof Number) {
            return Optional.of(((Number) object.get()).doubleValue());
        } else {
            throw new FunctionGatewayException("MathCalculatorFunction.Parameter.number not is Number");
        }
    }

    static private List<Double> createNumbers(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("numbers"));
        if (object.isEmpty()) {
            return new ArrayList<>();
        }
        if (object.get() instanceof List<?> numbers) {
            if (numbers.stream().anyMatch(n -> !(n instanceof Number))) {
                throw new FunctionGatewayException("MathCalculatorFunction.Paraneters.numbers must be Number type");
            }
            return numbers.stream()
                    .map(n -> ((Number) n).doubleValue())
                    .collect(Collectors.toList());
        } else {
            throw new FunctionGatewayException("MathCalculatorFunction.Parameters.numbers not is Array<Number>");
        }
    }
    static private Builder builder(String operation, Double number) {
        return new Builder(operation, number);
    }

    static private class Builder {

        final String operation;
        final Double number;
        Double secondNumber;
        final List<Double> numbers;

        private Builder(String operation, Double number) {
            this.operation = operation;
            this.number = number;
            this.secondNumber = null;
            this.numbers = new ArrayList<>();
        }

        public Builder setSecondNumber(Double secondNumber) {
            this.secondNumber = secondNumber;
            return this;
        }

        public Builder addNumbers(List<Double> numbers) {
            this.numbers.addAll(numbers);
            return this;
        }

        public Builder addNumbers(Double numbers) {
            this.numbers.add(numbers);
            return this;
        }

        public Parameters build() {
            return new Parameters(
                    operation,
                    number,
                    ()->Optional.ofNullable(secondNumber),
                    numbers
            );
        }

    }

    final private String operation;
    final private Double number;
    final private Double secondNumber;
    final private List<Double> numbers;

    private Parameters(String operation, Double number, Supplier<Optional<Double>> secondNumber, List<Double> numbers) {
        this.operation = operation;
        this.number = number;
        this.secondNumber = secondNumber.get().orElse(null);
        this.numbers = numbers;
    }

    public String operation() {
        return operation;
    }

    public Double number() {
        return number;
    }

    public Optional<Double> secondNumber() {
        return Optional.ofNullable(secondNumber);
    }

    public List<Double> numbers() {
        return numbers;
    }

}

package io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.systeminfo;

import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class Parameters {

    static public Parameters create(Map<String, Object> args) throws FunctionGatewayException {
        final Builder builder = builder(createInfoType(args));
        createFormat(args).ifPresent(builder::setFormat);
        return builder.build();
    }

    static private String createInfoType(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("info_type"));
        if (object.isEmpty()) {
            throw new FunctionGatewayException("SystemInfo.infoType is null");
        }
        if (object.get() instanceof String) {
            return (String) object.get();
        } else {
            throw new FunctionGatewayException("SystemInfo.infoType not is String");
        }
    }

    static private Optional<String> createFormat(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("format"));
        if (object.isEmpty()) {
            return Optional.empty();
        }
        if (object.get() instanceof String) {
            return Optional.of((String) object.get());
        } else {
            throw new FunctionGatewayException("SystemInfo.format not is String");
        }
    }

    static private Builder builder(String infoType) {
        return new Builder(infoType);
    }

    static private class Builder {

        final String infoType;
        String format;

        private Builder(String infoType) {
            this.infoType = infoType;
            this.format = null;
        }

        public Builder setFormat(String format) {
            this.format = format;
            return this;
        }

        public Parameters build() {
            return new Parameters(infoType, ()->Optional.ofNullable(format));
        }

    }

    final private String infoType;
    final private String format;

    private Parameters(String infoType, Supplier<Optional<String>> format) {
        this.infoType = infoType;
        this.format = format.get().orElse(null);
    }

    public String infoType() {
        return infoType;
    }

    public Optional<String> format() {
        return Optional.ofNullable(format);
    }

}

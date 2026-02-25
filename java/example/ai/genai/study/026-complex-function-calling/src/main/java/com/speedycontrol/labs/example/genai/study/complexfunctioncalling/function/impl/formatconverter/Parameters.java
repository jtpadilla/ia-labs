package com.speedycontrol.labs.example.genai.study.complexfunctioncalling.function.impl.formatconverter;

import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.Map;
import java.util.Optional;

public class Parameters {

    static public Parameters create(Map<String, Object> args) throws FunctionGatewayException {
        final Builder builder = builder(createData(args), createTargetFormat(args));
        createIncludeMetadata(args).ifPresent(builder::setIncludeMetadata);
        return builder.build();
    }

    static private String createData(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("data"));
        if (object.isEmpty()) {
            throw new FunctionGatewayException("FormatConverter.data is null");
        }
        if (object.get() instanceof String) {
            return (String) object.get();
        } else {
            throw new FunctionGatewayException("FormatConverter.data not is String");
        }
    }

    static private String createTargetFormat(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("target_format"));
        if (object.isEmpty()) {
            throw new FunctionGatewayException("FormatConverter.target_format is null");
        }
        if (object.get() instanceof String) {
            return ((String) object.get());
        } else {
            throw new FunctionGatewayException("FormatConverter.target_format not is String");
        }
    }

    static private Optional<Boolean> createIncludeMetadata(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("include_metadata"));
        if (object.isEmpty()) {
            return Optional.empty();
        }
        if (object.get() instanceof Boolean) {
            return Optional.of((Boolean) object.get());
        } else {
            throw new FunctionGatewayException("FormatConverter.include_metadata not is Boolean");
        }
    }

    static private Builder builder(String query, String target_format) {
        return new Builder(query, target_format);
    }

    static private class Builder {

        final private String data;
        final private String targetFormat;
        private Boolean includeMetadata;

        private Builder(String data, String targetFormat) {
            this.data = data;
            this.targetFormat = targetFormat;
            this.includeMetadata = null;
        }

        public Builder setIncludeMetadata(Boolean includeMetadata) {
            this.includeMetadata = includeMetadata;
            return this;
        }

        public Parameters build() {
            return new Parameters(
                    data,
                    targetFormat,
                    includeMetadata == null
            );
        }

    }

    final private String data;
    final private String targetFormat;
    final private boolean includeMetadata;

    private Parameters(String data, String targetFormat, boolean includeMetadata) {
        this.data = data;
        this.targetFormat = targetFormat;
        this.includeMetadata = includeMetadata;
    }

    public String data() {
        return data;
    }

    public String targetFormat() {
        return targetFormat;
    }

    public boolean includeMetadata() {
        return includeMetadata;
    }

}

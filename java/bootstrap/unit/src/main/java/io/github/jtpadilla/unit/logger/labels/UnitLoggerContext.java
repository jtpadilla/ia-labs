package io.github.jtpadilla.unit.logger.labels;

import io.github.jtpadilla.unit.Unit;
import io.github.jtpadilla.unit.logger.IUnitLogger;
import io.github.jtpadilla.unit.logger.UnitLoggerService;

import java.util.Map;
import java.util.Optional;

public class UnitLoggerContext {

    static public Builder newBuilder(Unit unit) {
        return new Builder(unit);
    }

    static public Builder newBuilder(String unitKey) {
        return newBuilder(new Unit(unitKey));
    }

    public interface ContextBuilderRetriever {
        UnitLoggerContext build();
        IUnitLogger buildLogger();
    }

    static public class Builder implements ContextBuilderRetriever {

        final private Unit unit;
        private UnitTrace trace;

        public Builder(Unit unit) {
            this.unit = unit;
        }

        public Unit getUnit() {
            return unit;
        }

        public Builder setTrace(UnitTrace trace) {
            this.trace = trace;
            return this;
        }

        public Optional<UnitTrace> getTrace() {
            return Optional.ofNullable(trace);
        }

        public Builder cloneBuilder() {
            final Builder result = new Builder(unit);
            result.trace = trace;
            return result;
        }

        @Override
        public UnitLoggerContext build() {
            return new UnitLoggerContext(this);
        }

        @Override
        public IUnitLogger buildLogger() {
            return UnitLoggerService.getLogger(build());
        }

        public OperationBuilder setOperation(String operationKey) {
            return new OperationBuilder(this, new UnitOperation(operationKey));
        }

        public OperationBuilder setOperation(UnitOperation operation) {
            return new OperationBuilder(this, operation);
        }

    }

    static public class OperationBuilder implements ContextBuilderRetriever {

        final private Builder builder;
        final private UnitOperation operation;

        public OperationBuilder(Builder builder, UnitOperation operation) {
            this.builder = builder;
            this.operation = operation;
        }

        public Unit getUnit() {
            return builder.getUnit();
        }

        public Optional<UnitTrace> getTrace() {
            return builder.getTrace();
        }

        public OperationBuilder setTrace(UnitTrace unitTrace) {
            builder.setTrace(unitTrace);
            return this;
        }

        public UnitOperation getOperation() {
            return operation;
        }

        public OperationBuilder cloneBuilder() {
            return new OperationBuilder(builder.cloneBuilder(), operation);
        }

        @Override
        public UnitLoggerContext build() {
            return new UnitLoggerContext(this);
        }

        @Override
        public IUnitLogger buildLogger() {
            return UnitLoggerService.getLogger(build());
        }


        public FeatureBuilder setFeature(String featureKey) {
            return new FeatureBuilder(this, new UnitFeature(featureKey));
        }

        public FeatureBuilder setFeature(UnitFeature feature) {
            return new FeatureBuilder(this, feature);
        }

    }

    static public class FeatureBuilder implements ContextBuilderRetriever {

        final private OperationBuilder builder;
        final private UnitFeature feature;

        public FeatureBuilder(OperationBuilder builder, UnitFeature feature) {
            this.builder = builder;
            this.feature = feature;
        }

        public Unit getUnit() {
            return builder.getUnit();
        }

        public Optional<UnitTrace> getTrace() {
            return builder.getTrace();
        }

        public FeatureBuilder setTrace(UnitTrace unitTrace) {
            builder.setTrace(unitTrace);
            return this;
        }

        public UnitOperation getOperation() {
            return builder.getOperation();
        }

        public UnitFeature getFeature() {
            return feature;
        }

        public FeatureBuilder cloneBuilder() {
            return new FeatureBuilder(builder.cloneBuilder(), feature);
        }

        @Override
        public UnitLoggerContext build() {
            return new UnitLoggerContext(this);
        }

        @Override
        public IUnitLogger buildLogger() {
            return UnitLoggerService.getLogger(build());
        }

        public DetailBuilder setDetail(String detailKey) {
            return new DetailBuilder(this, new UnitDetail(detailKey));
        }

        public DetailBuilder setDetail(UnitDetail detail) {
            return new DetailBuilder(this, detail);
        }

    }

    static public class DetailBuilder implements ContextBuilderRetriever {

        final private FeatureBuilder builder;
        final private UnitDetail detail;

        public DetailBuilder(FeatureBuilder builder, UnitDetail detail) {
            this.builder = builder;
            this.detail = detail;
        }

        public Unit getUnit() {
            return builder.getUnit();
        }

        public Optional<UnitTrace> getTrace() {
            return builder.getTrace();
        }

        public DetailBuilder setTrace(UnitTrace unitTrace) {
            builder.setTrace(unitTrace);
            return this;
        }

        public UnitOperation getOperation() {
            return builder.getOperation();
        }

        public UnitFeature getFeature() {
            return builder.getFeature();
        }

        public UnitDetail getDetail() {
            return detail;
        }

        public DetailBuilder cloneBuilder() {
            return new DetailBuilder(builder.cloneBuilder(), detail);
        }

        @Override
        public UnitLoggerContext build() {
            return new UnitLoggerContext(this);
        }

        @Override
        public IUnitLogger buildLogger() {
            return UnitLoggerService.getLogger(build());
        }

    }

    final private Unit unit;
    final private UnitTrace trace;
    final private UnitOperation operation;
    final private UnitFeature feature;
    final private UnitDetail detail;

    private UnitLoggerContext(Builder contextBuilder) {
        this.unit = contextBuilder.getUnit();
        this.trace = contextBuilder.getTrace().orElse(null);
        this.operation = null;
        this.feature = null;
        this.detail = null;
    }

    private UnitLoggerContext(OperationBuilder contextOperationBuilder) {
        this.unit = contextOperationBuilder.getUnit();
        this.trace = contextOperationBuilder.getTrace().orElse(null);
        this.operation = contextOperationBuilder.getOperation();
        this.feature = null;
        this.detail = null;
    }

    private UnitLoggerContext(FeatureBuilder contextFeatureBuilder) {
        this.unit = contextFeatureBuilder.getUnit();
        this.trace = contextFeatureBuilder.getTrace().orElse(null);
        this.operation = contextFeatureBuilder.getOperation();
        this.feature = contextFeatureBuilder.getFeature();
        this.detail = null;
    }

    private UnitLoggerContext(DetailBuilder contextDetailBuilder) {
        this.unit = contextDetailBuilder.getUnit();
        this.trace = contextDetailBuilder.getTrace().orElse(null);
        this.operation = contextDetailBuilder.getOperation();
        this.feature = contextDetailBuilder.getFeature();
        this.detail = contextDetailBuilder.getDetail();
    }

    public Unit getUnit() {
        return unit;
    }

    public Map<String, String> getLabels() {
        final Map<String, String> result = new java.util.HashMap<>();
        if (operation != null) {
            result.put("operation", operation.name());
        }
        if (feature != null) {
            result.put("feature", feature.name());
        }
        if (detail != null) {
            result.put("detail", detail.name());
        }
        return result;
    }

    public Optional<UnitTrace> getTrace() {
        return Optional.ofNullable(trace);
    }

}

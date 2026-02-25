package io.github.jtpadilla.unit.logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class UnitLoggerData {

    public record StructEntry(String key, Value value) {}

    static public class Builder {

        final private UnitLoggerLevel level;
        final private String msg;
        final private ArrayList<StructEntry> structEntries;

        private Throwable throwable;

        private Builder(UnitLoggerLevel level, String msg) {
            this.level = Objects.requireNonNull(level, "'level' must not be null");
            this.msg = Objects.requireNonNull(msg, "'msg' must not be null");
            this.structEntries = new ArrayList<>();
        }

        public Builder addValue(String name, Value value) {
            structEntries.add(new StructEntry(name, value));
            return this;
        }

        public Builder addMessage(String name, Message message) {
            try {
                // 1. Message to JSON
                final String jsonString = JsonFormat.printer().print(message);
                // 2. JSON to Struct
                final Struct.Builder structBuilder = Struct.newBuilder();
                JsonFormat.parser().ignoringUnknownFields().merge(jsonString, structBuilder);
                final Struct struct = structBuilder.build();
                // 3. Crear un Value a partir del Struct
                final Value value = Value.newBuilder().setStructValue(struct).build();
                // 4. UnitLoggerData
                addValue(name, value);
            } catch (InvalidProtocolBufferException e) {
                final Value conversionValue = Value.newBuilder()
                        .setStringValue(String.format("Unable to convert message '%s' to Struct", message))
                        .build();
                addValue(name, conversionValue);
            }
            return this;
        }

        public Builder setThrowable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public UnitLoggerData build() {
            return new UnitLoggerData(
                    level,
                    msg,
                    structEntries,
                    throwable
            );
        }

    }

    static public Builder newBuilder(UnitLoggerLevel level, String msg) {
        return new Builder(level, msg);
    }

    final private UnitLoggerLevel level;
    final private String msg;
    final private ArrayList<StructEntry> structEntries;
    final private Throwable throwable;

    private UnitLoggerData(
            UnitLoggerLevel level,
            String msg,
            ArrayList<StructEntry> structEntries,
            Throwable throwable) {
        this.level = level;
        this.msg = msg;
        this.structEntries = structEntries;
        this.throwable = throwable;
    }

    public UnitLoggerLevel getLevel() {
        return level;
    }

    public String getMsg() {
        return msg;
    }

    public List<StructEntry> getStructEntries() {
        return structEntries;
    }

    public Optional<Throwable> getThrowable() {
        return Optional.ofNullable(throwable);
    }

}

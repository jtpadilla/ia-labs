package io.github.jtpadilla.protobuf;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.github.jtpadilla.util.TimeUtil;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class TimestampUtil {

    private static final ZoneId ZONEID_UTC = ZoneId.of("UTC");

    static public OldestOptionalBuilder oldest() {
        return new OldestOptionalBuilder();
    }

    static public OldestRequiredBuilder oldest(Timestamp required) {
        return new OldestRequiredBuilder(required);
    }

    public static class OldestOptionalBuilder {

        final private List<Timestamp> list;

        OldestOptionalBuilder() {
            this.list = new ArrayList<>();
        }

        public OldestOptionalBuilder add(Supplier<Boolean> isProvided, Supplier<Timestamp> timestampSupplier) {
            if (isProvided.get()) {
                list.add(timestampSupplier.get());
            }
            return this;
        }

        public OldestOptionalBuilder add(Timestamp timestamp) {
            if (timestamp != null) {
                list.add(timestamp);
            }
            return this;
        }

        public OldestOptionalBuilder add(List<Timestamp> timestampList) {
            list.addAll(timestampList);
            return this;
        }

        public Optional<Timestamp> calculate() {
            Timestamp result = null;
            for (Timestamp ts : list) {
                if (result == null) {
                    result = ts;
                } else {
                    if (Timestamps.compare(ts, result) > 0) {
                        result = ts;
                    }
                }
            }
            return Optional.ofNullable(result);
        }

    }

    public static class OldestRequiredBuilder {

        final private List<Timestamp> list;
        final private Timestamp required;

        OldestRequiredBuilder(Timestamp required) {
            this.list = new ArrayList<>();
            this.required = required;
        }

        public OldestRequiredBuilder add(Supplier<Boolean> isProvided, Supplier<Timestamp> timestampSupplier) {
            if (isProvided.get()) {
                list.add(timestampSupplier.get());
            }
            return this;
        }

        public OldestRequiredBuilder add(Timestamp timestamp) {
            if (timestamp != null) {
                list.add(timestamp);
            }
            return this;
        }

        public Timestamp calculate() {
            Objects.requireNonNull(required, "La creacion del contetexto no puede ser nula.");
            Timestamp result = required;
            for (Timestamp ts : list) {
                if (Timestamps.compare(ts, result) > 0) {
                    result = ts;
                }
            }
            return result;
        }

    }

    // PROTOBUF Duration

    static public com.google.protobuf.Duration javaDurationToProtoDuration(java.time.Duration javaDuration) {

        java.time.Duration javaSecondsDuration = java.time.Duration.ofSeconds(javaDuration.getSeconds());
        java.time.Duration javaNanosDuration = javaDuration.minus(javaSecondsDuration);

        return Duration.newBuilder()
                .setSeconds(javaSecondsDuration.getSeconds())
                .setNanos(javaNanosDuration.getNano())
                .build();

    }

    // PROTOBUF Timestamp

    static public Timestamp localDateTimeToTimestamp(LocalDateTime localDateTime) {

        final Instant instant = localDateTime.toInstant(ZoneOffset.UTC);

        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();

    }

    static public Timestamp instantToTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    static public Timestamp nowToTimestamp() {
        return localDateTimeToTimestamp(TimeUtil.nowLocalDateTime());
    }

    // JAVA Instant

    static public Instant timestampToInstant(Timestamp ts) {
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }

    // JAVA LocalDateTime (siempre en UTC)

    static public LocalDateTime timestampToLocalDatetime(Timestamp ts) {
        return LocalDateTime.ofEpochSecond(ts.getSeconds(), ts.getNanos(), ZoneOffset.UTC);
    }

    // JAVA ZonedDateTime (siempre en UTC)

    static public ZonedDateTime timestampToZonedDatetime(Timestamp timestamp) {
        return ZonedDateTime.of(timestampToLocalDatetime(timestamp), ZONEID_UTC);
    }

    // toString

    static public String formatTimestamp(Timestamp timestamp) {
        return TimeUtil.formatLocalDataTime(timestampToLocalDatetime(timestamp));
    }
    
}

package io.github.jtpadilla.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    final static private DateTimeFormatter DATETIMEFORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnn");

    final static private ZoneId ZONEID_UTC = ZoneId.of("UTC");

    static public LocalDateTime instantToLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    static public LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now(ZONEID_UTC);
    }

    static public String formatLocalDataTime(LocalDateTime localDateTime) {
        return localDateTime.format(DATETIMEFORMATTER);
    }


}

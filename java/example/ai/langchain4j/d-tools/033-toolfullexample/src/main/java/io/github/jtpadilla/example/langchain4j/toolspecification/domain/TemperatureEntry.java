package io.github.jtpadilla.example.langchain4j.toolspecification.domain;

import java.time.LocalDateTime;

public record TemperatureEntry(
        LocalDateTime localDateTime,
        String city,
        double temperature) {
}

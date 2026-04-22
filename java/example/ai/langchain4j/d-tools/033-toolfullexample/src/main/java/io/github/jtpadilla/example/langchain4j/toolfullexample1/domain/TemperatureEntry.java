package io.github.jtpadilla.example.langchain4j.toolfullexample1.domain;

import java.time.LocalDateTime;

public record TemperatureEntry(
        LocalDateTime localDateTime,
        String city,
        double temperature) {
}

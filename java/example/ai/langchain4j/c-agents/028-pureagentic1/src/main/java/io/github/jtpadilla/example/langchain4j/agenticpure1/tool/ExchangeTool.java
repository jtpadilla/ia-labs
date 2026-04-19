package io.github.jtpadilla.example.langchain4j.agenticpure1.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Map;

public class ExchangeTool {

    // Rates relative to USD (approx. 2025-04)
    private static final Map<String, Double> RATES_TO_USD = Map.of(
            "USD", 1.0,
            "EUR", 1.08,
            "GBP", 1.27,
            "JPY", 0.0067,
            "CHF", 1.12,
            "CAD", 0.73,
            "AUD", 0.64,
            "MXN", 0.052
    );

    @Tool("Convierte la cantidad indicada de dinero de la divisa original a la divisa destino")
    Double exchange(
            @P("originalCurrency") String originalCurrency,
            @P("amount") Double amount,
            @P("targetCurrency") String targetCurrency) {

        Double fromRate = RATES_TO_USD.get(originalCurrency.toUpperCase());
        Double toRate = RATES_TO_USD.get(targetCurrency.toUpperCase());

        if (fromRate == null) {
            throw new IllegalArgumentException("Unsupported currency: " + originalCurrency);
        }
        if (toRate == null) {
            throw new IllegalArgumentException("Unsupported currency: " + targetCurrency);
        }

        double amountInUsd = amount * fromRate;
        return amountInUsd / toRate;
    }

}

package io.github.jtpadilla.example.helidon.injection.programmatic.service.payment.impl;

import io.github.jtpadilla.example.helidon.injection.programmatic.service.payment.PaymentProcessor;

/**
 * Procesador de emergencia con peso superior para sobreescribir el por defecto.
 * No tiene anotaciones.
 */
public class EmergencyPaymentProcessor implements PaymentProcessor {
    @Override
    public String process(double amount) {
        return "[ALERTA] Procesando pago crítico de " + amount + " via " + providerName();
    }

    @Override
    public String providerName() {
        return "Emergency-Channel";
    }
}

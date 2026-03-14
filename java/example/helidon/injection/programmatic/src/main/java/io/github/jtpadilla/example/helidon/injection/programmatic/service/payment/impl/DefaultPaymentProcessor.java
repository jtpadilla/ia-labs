package io.github.jtpadilla.example.helidon.injection.programmatic.service.payment.impl;

import io.github.jtpadilla.example.helidon.injection.programmatic.service.payment.PaymentProcessor;
import io.helidon.service.registry.Service;
import org.jspecify.annotations.NullMarked;

/**
 * Implementación por defecto registrada automáticamente mediante anotaciones.
 * Será parte del ApplicationBinding generado.
 */
@Service.Singleton
@NullMarked
public class DefaultPaymentProcessor implements PaymentProcessor {

    @Override
    public String process(double amount) {
        return "Procesando " + amount + " via " + providerName();
    }

    @Override
    public String providerName() {
        return "Standard-Gateway";
    }
}

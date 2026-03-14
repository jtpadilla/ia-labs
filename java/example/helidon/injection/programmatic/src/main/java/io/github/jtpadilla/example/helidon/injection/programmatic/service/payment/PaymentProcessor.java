package io.github.jtpadilla.example.helidon.injection.programmatic.service.payment;

/**
 * Contrato base para procesadores de pagos.
 */
public interface PaymentProcessor {
    String process(double amount);
    String providerName();
}

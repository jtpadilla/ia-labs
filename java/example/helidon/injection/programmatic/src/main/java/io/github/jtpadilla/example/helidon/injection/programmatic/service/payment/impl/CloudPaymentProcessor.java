package io.github.jtpadilla.example.helidon.injection.programmatic.service.payment.impl;

import io.github.jtpadilla.example.helidon.injection.programmatic.service.hsm.HSMService;
import io.github.jtpadilla.example.helidon.injection.programmatic.service.payment.PaymentProcessor;
import org.jspecify.annotations.NullMarked;

/**
 * Procesador en la nube que depende del HSM.
 * Se registrará vía VirtualDescriptor.
 */
@NullMarked
public class CloudPaymentProcessor implements PaymentProcessor {
    private final HSMService hsm;

    public CloudPaymentProcessor(HSMService hsm) {
        this.hsm = hsm;
        System.out.println("[Cloud] Procesador instanciado perezosamente.");
    }

    @Override
    public String process(double amount) {
        String signature = hsm.sign("PAYLOAD:" + amount);
        return "Pago en la nube de " + amount + " firmado con " + signature;
    }

    @Override
    public String providerName() {
        return "GCP-Cloud-Payment";
    }
}

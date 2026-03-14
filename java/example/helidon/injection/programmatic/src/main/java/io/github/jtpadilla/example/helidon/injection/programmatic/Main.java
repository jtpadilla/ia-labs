package io.github.jtpadilla.example.helidon.injection.programmatic;

import io.github.jtpadilla.example.helidon.injection.programmatic.service.hsm.HSMService;
import io.github.jtpadilla.example.helidon.injection.programmatic.service.hsm.impl.HSMImpl;
import io.github.jtpadilla.example.helidon.injection.programmatic.service.payment.PaymentProcessor;
import io.github.jtpadilla.example.helidon.injection.programmatic.service.payment.impl.CloudPaymentProcessor;
import io.github.jtpadilla.example.helidon.injection.programmatic.service.payment.impl.EmergencyPaymentProcessor;
import io.helidon.service.registry.ServiceRegistryConfig;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.service.registry.ServiceRegistry;
import io.helidon.service.registry.ExistingInstanceDescriptor;
import io.helidon.service.registry.Services;

import java.util.Set;

/**
 * Ejemplo avanzado de gestión programática del Registro de Servicios de Helidon 4.3.0.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== Helidon Programmatic Service Registry Demo ===");

        // 1. PREPARACIÓN DE INSTANCIAS EXISTENTES (Infraestructura)
        HSMImpl hsm = new HSMImpl("europe-west1");
        hsm.init();

        // 2. CONFIGURACIÓN DEL REGISTRO
        ServiceRegistryConfig config = ServiceRegistryConfig.builder()
                // A. HSM Service
                .addServiceDescriptor(ExistingInstanceDescriptor.create(
                        hsm,
                        Set.of(HSMService.class),
                        100.0
                ))
                // B. Cloud Payment Processor (Peso 110.0)
                .addServiceDescriptor(ExistingInstanceDescriptor.create(
                        new CloudPaymentProcessor(hsm),
                        Set.of(PaymentProcessor.class),
                        110.0
                ))
                // C. Emergency Payment Processor (Peso 200.0)
                .addServiceDescriptor(ExistingInstanceDescriptor.create(
                        new EmergencyPaymentProcessor(),
                        Set.of(PaymentProcessor.class),
                        200.0
                ))
                .build();

        // 3. ARRANQUE DEL GESTOR
        ServiceRegistryManager manager = ServiceRegistryManager.start(config);
        ServiceRegistry registry = manager.registry();

        // 4. INTERVENCIÓN DE ÚLTIMO MOMENTO (Services.set)
        // CRÍTICO: Debe hacerse ANTES de cualquier llamada a registry.get(PaymentProcessor.class)
        System.out.println("\n-> Aplicando Monkey-patching vía Services.set (ANTES del primer acceso)...");
        PaymentProcessor mockProcessor = new PaymentProcessor() {
            @Override public String process(double amount) { return "MOCK PROCESSED " + amount; }
            @Override public String providerName() { return "Mock-Patch"; }
        };
        // Esto sobrescribirá incluso a EmergencyPaymentProcessor porque Services.set tiene prioridad absoluta
        Services.set(PaymentProcessor.class, mockProcessor);

        // 5. VERIFICACIÓN DE RESOLUCIÓN
        // Aunque Emergency tiene peso 200, el Services.set (Monkey-patch) gana por ser una intervención directa
        PaymentProcessor processor = registry.get(PaymentProcessor.class);
        System.out.println("-> Proveedor resuelto (Esperado: Mock-Patch): " + processor.providerName());
        System.out.println("   Result: " + processor.process(1500.50));

        // 6. VERIFICACIÓN DE INFRAESTRUCTURA COMPARTIDA
        HSMService sharedHsm = registry.get(HSMService.class);
        System.out.println("\n-> HSM extraído del registro: " + (sharedHsm == hsm ? "MISMA INSTANCIA" : "NUEVA (ERROR)"));
        System.out.println("   Firma HSM: " + sharedHsm.sign("TEST-DATA"));

        // 7. CIERRE ORDENADO
        System.out.println("\n=== Apagando Registro ===");
        manager.shutdown();
        
        hsm.close();
        
        System.out.println("Demo finalizada correctamente.");
    }
}

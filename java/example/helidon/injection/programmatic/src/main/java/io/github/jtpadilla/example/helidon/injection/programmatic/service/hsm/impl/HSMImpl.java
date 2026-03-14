package io.github.jtpadilla.example.helidon.injection.programmatic.service.hsm.impl;

import io.github.jtpadilla.example.helidon.injection.programmatic.service.hsm.HSMService;

/**
 * Mock de HSM que no tiene anotaciones. Se registrará como una instancia existente.
 */
public class HSMImpl implements HSMService {
    private final String region;
    private boolean initialized = false;

    public HSMImpl(String region) {
        this.region = region;
    }

    public void init() {
        this.initialized = true;
        System.out.println("[HSM] Inicializado en región: " + region);
    }

    @Override
    public String sign(String data) {
        if (!initialized) throw new IllegalStateException("HSM no inicializado");
        return "SIG(" + data + ")[" + region + "]";
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    public void close() {
        System.out.println("[HSM] Cerrando conexiones de seguridad...");
        this.initialized = false;
    }
}

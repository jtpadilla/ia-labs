package io.github.jtpadilla.example.helidon.injection.programmatic.service.hsm;

/**
 * Servicio de seguridad crítico (Hardware Security Module).
 * Este servicio requiere una inicialización compleja y será registrado manualmente.
 */
public interface HSMService {
    String sign(String data);
    boolean isInitialized();
}

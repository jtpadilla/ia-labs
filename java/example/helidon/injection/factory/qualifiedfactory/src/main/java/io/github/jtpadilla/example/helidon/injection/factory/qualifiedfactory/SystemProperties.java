package io.github.jtpadilla.example.helidon.injection.factory.qualifiedfactory;

import io.helidon.service.registry.Service;

// Esta es una clase que para crear una instancia requiere que se le inyecten
// dos strings con los valores que corresponden a las propiedades del sistema.
@Service.Singleton
public class SystemProperties {

    private final String httpHost;
    private final String httpPort;

    SystemProperties(@SystemProperty("http.host") String httpHost,
                     @SystemProperty("http.port") String httpPort) {
        this.httpHost = httpHost;
        this.httpPort = httpPort;
    }

    @Override
    public String toString() {
        return "SystemProperties{" +
                "httpHost='" + httpHost + '\'' +
                ", httpPort='" + httpPort + '\'' +
                '}';
    }

}
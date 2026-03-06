package io.github.jtpadilla.example.helidon.injection.factory.qualifiedfactory;

import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

@Service.Singleton
public class Main {

    public static void main(String[] args) {

        // Asigna las propiedades del sistema
        System.setProperty("http.host", "localhost");
        System.setProperty("http.port", "8080");

        // Mediante el registro central de servicios (via inyección) se obtiene una instancia de main.
        Main main = Services.get(Main.class);
        System.out.println("Inyectado: " + main.props);
    }

    private final SystemProperties props;

    // Esta clase para construirse pedirá que se le inyecte una instancia de SystemProperties
    @Service.Inject
    public Main(SystemProperties props) {
        this.props = props;
    }

}
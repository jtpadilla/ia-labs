package example.dagger;

import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * El Componente es la interfaz central de Dagger.
 * Define la raíz del grafo de dependencias y los objetos que se pueden obtener de él.
 * 
 * @Singleton asegura que el monitor sea una instancia única dentro del ciclo de vida del componente.
 * modules especifica los módulos que contribuyen al grafo.
 */
@Singleton
@Component(modules = {HealthCheckModule.class})
public interface HealthMonitorComponent {

    /**
     * Punto de entrada para obtener la lógica de negocio principal.
     */
    HealthMonitor getHealthMonitor();

    /**
     * La Factoría permite personalizar la creación del componente.
     * Es preferible a @Component.Builder por su seguridad de tipos en los parámetros.
     */
    @Component.Factory
    interface Factory {
        /**
         * @BindsInstance permite inyectar objetos que ya existen (como configuraciones de entorno)
         * directamente al grafo sin necesidad de un Módulo.
         * 
         * @Named se usa para distinguir entre múltiples Strings inyectados.
         */
        HealthMonitorComponent create(@BindsInstance @Named("environment") String environment);
    }

}


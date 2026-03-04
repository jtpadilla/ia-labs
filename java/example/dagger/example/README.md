# Ejemplo de Dagger 2 con Bazel

Este ejemplo demuestra una implementación compleja de **Dagger 2** utilizando **Bazel**, enfocada en un sistema de monitoreo de salud extensible.

## Arquitectura del Ejemplo

El sistema se compone de varios "Health Checks" (CPU, Memoria, Disco) que se registran automáticamente en un monitor central mediante mecanismos avanzados de inyección de dependencias.

### Conceptos de Dagger 2 Demostrados

1.  **Componentes y Factorías (`HealthMonitorComponent`)**:
    *   Uso de `@Component.Factory` para una creación de componentes más clara y segura.
    *   `@BindsInstance`: Permite pasar objetos externos (como el nombre del entorno) al grafo de dependencias en tiempo de ejecución.

2.  **Multibindings (`HealthCheckModule`)**:
    *   `@IntoSet`: Permite que múltiples módulos (o métodos) contribuyan elementos a un `Set<T>`. El `HealthMonitor` recibe todos los chequeos sin conocerlos individualmente.
    *   `@IntoMap` / `@StringKey`: Permite inyectar un `Map<String, T>`, ideal para buscar componentes específicos por una clave.

3.  **Módulos y Vinculación (`HealthCheckModule`)**:
    *   `@Binds`: Una forma eficiente de delegar una interfaz a su implementación (más ligero que `@Provides`).
    *   `@Provides`: Se utiliza para lógica de creación más compleja o cuando se requiere transformar dependencias.

4.  **Calificadores (`@FastCheck`)**:
    *   Uso de anotaciones personalizadas con `@Qualifier` para diferenciar entre múltiples instancias del mismo tipo (por ejemplo, "todos los chequeos" vs "solo los chequeos rápidos").

5.  **Ámbitos (`@Singleton`)**:
    *   Garantiza que las instancias (como los sensores o el monitor) se compartan y no se recreen innecesariamente.

## Configuración de Bazel

Para que Dagger funcione, Bazel debe ejecutar el procesador de anotaciones durante la compilación:

*   **`java_plugin`**: Define el procesador `dagger.internal.codegen.ComponentProcessor`.
*   **`plugins` en `java_library`**: Vincula el plugin a la librería para que genere el código de las factorías y componentes (clases que empiezan por `Dagger...`).

## Cómo Ejecutar

Desde la raíz del proyecto:

```bash
bazel run //java/example/dagger:dagger_example
```

## Estructura de Archivos

- `checks/`: Interfaces e implementaciones de los sensores de salud.
- `qualifiers/`: Anotaciones personalizadas para filtrado de dependencias.
- `HealthCheckModule.java`: Configuración del grafo de objetos.
- `HealthMonitorComponent.java`: Puerta de entrada al sistema de inyección.
- `HealthMonitor.java`: Lógica de negocio que consume las dependencias.

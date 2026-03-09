# Gemini Project: ia-labs

Este repositorio es un entorno experimental para la investigación y desarrollo con la IA Generativa de Google (Gemini) y Vertex AI. Aquí se prueban integraciones, se desarrollan prototipos de agentes y se exploran las capacidades de los modelos de Google Cloud.

## Mandatos Fundamentales

*   **Bazel 8.x (Bzlmod):** El proyecto utiliza exclusivamente **Bazel 8.x** para la gestión de dependencias y construcción, empleando el sistema de módulos (**Bzlmod**).
*   **Helidon SE:** Este es un proyecto multimódulo de **Helidon 4** diseñado para explorar las diferentes capacidades del framework, enfocado exclusivamente en **Helidon SE**. En ningún caso se permitirá el uso de Helidon MP (MicroProfile).
*   **Inyección de Dependencias:** Se utilizará el **Service Registry** nativo de Helidon 4 SE para la gestión de dependencias.
*   **Anotaciones:** Se utilizará **JSpecify** (`org.jspecify.annotations`) como estándar para anotaciones de nulidad y tipos en todo el proyecto.
*   **Estándar de Java:** Java 21 (mínimo).
*   **Validación del Agente:** El agente **NO** debe ejecutar comandos de Bazel (`build`, `test`, `run`, etc.) de forma proactiva para validar cambios. Debido a que el agente y IntelliJ utilizan cachés distintas, la ejecución desde el agente obligaría a reconstruir el proyecto en el IDE. La validación con Bazel se realizará únicamente bajo petición explícita del usuario.
*   **Experimental:** El código puede sufrir refactorizaciones drásticas. Prioriza la exploración y el aprendizaje sobre la estabilidad a largo plazo en las capas de `example/`.

## Arquitectura del Proyecto

El proyecto está organizado en capas para facilitar la reutilización y el aislamiento:

| Capa | Ubicación | Propósito |
|---|---|---|
| **Bootstrap** | `/java/bootstrap` | Utilidades base (Gson, Protobuf), gestión de contextos de GCloud y el sistema `Unit`. |
| **A2A** | `/java/a2a` | Comunicación Agente a Agente: modelos, motores de limpieza, despacho y repositorios. |
| **Platform** | `/java/platform` | Frameworks de agentes reutilizables y servidores de despacho (ianews, laliga). |
| **Product** | `/java/product` | Aplicaciones finales listas para producción (iatevaleagent). |
| **Example** | `/java/example` | Cookbook con más de 30 ejemplos: AI (Gemini, Vertex), Helidon SE, Xodus, UI Javelit, Telegram. |
| **Lib** | `/java/lib` | Librerías compartidas y utilidades comunes del proyecto. |
| **Third Party** | `/java/third_party` | Adaptaciones o extensiones de librerías externas (GenAI, Closeable, Interactions). |
| **Proto** | `/proto` | Definiciones de Protocol Buffers (gRPC, dominio) organizadas por namespace. |

## Tecnologías Clave

*   **IA de Google:** Vertex AI SDK, Google GenAI SDK (Gemini) y Agent Development Kit (ADK).
*   **Servicios:** Helidon 4 SE (Webserver, Service Registry, Logging).
*   **Persistencia:** JetBrains Xodus (KV Store, Entity Store, VFS) y Google Cloud Datastore.
*   **Inyección de Dependencias:** Helidon Service Registry, Google Dagger 2 y Jakarta Inject.
*   **UI & Datos:** Javelit (prototipado UI Java) y Tablesaw (análisis de datos).
*   **Mensajería:** Telegram Bots SDK (Long Polling).
*   **Construcción:** Bazel (vía `MODULE.bazel` para dependencias externas).

## Convenciones de Desarrollo

*   **Estructura multimódulo:** Diseñada para facilitar la adición de nuevos experimentos de forma aislada en paquetes Bazel.
*   **Java Moderno:** Uso de Java 21 y sus características modernas (Virtual Threads, Pattern Matching).
*   **Configuración Centralizada:** Gestión de dependencias centralizada en el archivo raíz `MODULE.bazel`.
*   **Sistema Unit:** Se utiliza la clase `Unit` para identificar componentes, gestionar entornos y configurar el logging de forma centralizada.
*   **Wrappers de GenAI:** Se prefiere el uso de abstracciones sobre los SDKs oficiales para mantener una interfaz consistente en todo el proyecto.
*   **Despliegue OCI:** Las aplicaciones se empaquetan usando las reglas `rules_oci` de Bazel y se despliegan directamente en Artifact Registry.

## Configuración del Entorno

Para ejecutar los ejemplos y productos, se requiere:

1.  **Credenciales de GCloud:** Un Service Account con roles de `Storage Admin`, `Cloud Datastore User` y `Vertex AI User`.
2.  **Archivo de Configuración:** Crear `~/.iatevale/config.properties` con `project.id` y la ruta a las `credentials` (JSON).
3.  **Bazel:** Utilizar `bazel build //...` para compilar y `bazel run` para ejecutar targets específicos.
4.  **Telegram (Opcional):** Configurar `TELEGRAM_BOT_TOKEN` en el entorno para ejemplos de mensajería.

---
*Este documento sirve como guía para desarrolladores y agentes de IA que trabajen en este repositorio.*

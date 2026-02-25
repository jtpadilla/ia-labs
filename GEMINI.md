# Gemini Project: ia-labs

Este repositorio es un entorno experimental para la investigación y desarrollo con la IA Generativa de Google (Gemini) y Vertex AI. Aquí se prueban integraciones, se desarrollan prototipos de agentes y se exploran las capacidades de los modelos de Google Cloud.

## Mandatos Fundamentales

*   **Sin Maven:** No utilices nunca el comando `mvn`. Este proyecto utiliza exclusivamente **Bazel** para la gestión de dependencias y construcción.
*   **Experimental:** El código puede sufrir refactorizaciones drásticas. Prioriza la exploración y el aprendizaje sobre la estabilidad a largo plazo en las capas de `example/`.

## Arquitectura del Proyecto

El proyecto está organizado en capas para facilitar la reutilización y el aislamiento:

| Capa | Ubicación | Propósito |
|---|---|---|
| **Bootstrap** | `/java/bootstrap` | Utilidades base, gestión de contextos de GCloud y el sistema `Unit` para configuración y logging. |
| **Platform** | `/java/platform` | Frameworks de agentes reutilizables y componentes de servidor inyectables con Guice. |
| **Product** | `/java/product` | Aplicaciones finales empaquetadas como imágenes OCI para su despliegue en Cloud Run. |
| **Example** | `/java/example` | Un "cookbook" extenso con más de 27 ejemplos de uso de la API de Gemini (streaming, function calling, embeddings, etc.). |
| **Proto** | `/proto` | Definiciones de Protocol Buffers para estructuras de datos y comunicación gRPC. |

## Tecnologías Clave

*   **Lenguaje:** Java 21/25 (uso intensivo de características modernas).
*   **Construcción:** Bazel (vía `MODULE.bazel` para dependencias externas).
*   **Inyección de Dependencias:** Google Guice.
*   **IA de Google:** Vertex AI SDK, Google GenAI SDK, y Agent Development Kit (ADK).
*   **Infraestructura:** Google Cloud (Datastore, Storage, Discovery Engine, Cloud Run).
*   **Comunicación:** Protobuf y gRPC.

## Convenciones de Desarrollo

*   **Sistema Unit:** Se utiliza la clase `Unit` para identificar componentes, gestionar entornos y configurar el logging de forma centralizada.
*   **Wrappers de GenAI:** Se prefiere el uso de abstracciones sobre los SDKs oficiales para mantener una interfaz consistente en todo el proyecto.
*   **Despliegue OCI:** Las aplicaciones se empaquetan usando las reglas `rules_oci` de Bazel y se despliegan directamente en Artifact Registry.

## Configuración del Entorno

Para ejecutar los ejemplos y productos, se requiere:

1.  **Credenciales de GCloud:** Un Service Account con roles de `Storage Admin`, `Cloud Datastore User` y `Vertex AI User`.
2.  **Archivo de Configuración:** Crear `~/.iatevale/config.properties` con `project.id` y la ruta a las `credentials` (JSON).
3.  **Bazel:** Utilizar `bazel build //...` para compilar y `bazel run` para ejecutar targets específicos.

---
*Este documento sirve como guía para desarrolladores y agentes de IA que trabajen en este repositorio.*


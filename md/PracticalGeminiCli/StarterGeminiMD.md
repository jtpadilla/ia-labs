# Guía del Archivo GEMINI.md para la CLI

El archivo `GEMINI.md` es un componente crucial al trabajar con la CLI (Interfaz de Línea de Comandos) de Gemini, sirviendo como un espacio dedicado para proporcionar instrucciones específicas del proyecto y contexto al modelo Gemini.

## Importancia de GEMINI.md

Piensa en el archivo `GEMINI.md` como una "hoja de trucos" (cheat sheet) para Gemini, guiando su comportamiento y asegurando que sus respuestas se alineen con los requisitos únicos de tu proyecto. Al crear este archivo en el directorio raíz de tu repositorio, puedes definir:

* **Convenciones de Código:** Especifica tu estilo de codificación preferido, como reglas de formato, convenciones de nomenclatura y mejores prácticas. Esto asegura que cualquier código generado o modificado por Gemini se adhiera a los estándares de tu proyecto.
* **Patrones Arquitectónicos:** Describe los principios arquitectónicos de tu proyecto. Esto ayuda a Gemini a comprender la estructura y el diseño general, permitiéndole generar código que sea coherente con tu arquitectura existente.
* **Directrices Específicas del Proyecto:** Proporciona cualquier otra información relevante que Gemini deba conocer, como una lista de archivos importantes, dependencias clave o instrucciones específicas para interactuar con tu base de código.

Al proporcionar este contexto, puedes mejorar significativamente la precisión y relevancia de los resultados de Gemini, convirtiéndolo en un asistente más eficaz e inteligente para tus tareas de desarrollo de software.

---

## Creación automática de un archivo GEMINI.md

Aunque puedes, y deberías, crear un archivo `GEMINI.md` para cualquier proyecto nuevo que inicies, su importancia es aún más pronunciada cuando se trabaja con repositorios existentes.

Para cualquier proyecto que no se haya iniciado con la CLI de Gemini, necesitarás crear un archivo `GEMINI.md` desde cero. Este proceso manual implica documentar las convenciones de codificación del proyecto, los patrones arquitectónicos y otras directrices específicas para aprovechar al máximo la CLI de Gemini.

Sin embargo, aquí es donde entra en juego una potente función potencial para la CLI de Gemini: generar automáticamente un archivo `GEMINI.md` analizando la estructura y la base de código existentes del proyecto.

La CLI de Gemini podría entonces:
1.  Analizar el árbol de carpetas.
2.  Identificar los lenguajes de programación y frameworks en uso.
3.  Analizar el código para inferir estilos de codificación (ej. tabulaciones vs. espacios, convenciones de nomenclatura).

Esto no solo ahorra un tiempo significativo al desarrollador, sino que también asegura que las contribuciones de Gemini se alineen perfectamente con los patrones establecidos del proyecto, haciendo que la herramienta sea inmediatamente relevante.

---

## El Prompt (La Indicación)

A continuación se presenta un prompt diseñado para "arrancar" (bootstrap) un archivo `GEMINI.md` analizando un repositorio existente. Puedes usar el siguiente texto como instrucción para Gemini:

> **Instrucción para la IA:**
>
> Eres un experto arquitecto de software y asistente de análisis de proyectos. Analiza el directorio del proyecto actual de forma recursiva y genera un archivo `GEMINI.md` completo. Este archivo servirá como una guía de contexto fundamental para cualquier futuro modelo de IA, como tú, que interactúe con este proyecto. El objetivo es asegurar que el código, el análisis y las modificaciones generadas por la IA en el futuro sean consistentes con los estándares y la arquitectura establecidos en el proyecto.
>
> 1.  **Escanear y Analizar:** Escanea recursivamente toda la estructura de archivos y carpetas comenzando desde el directorio raíz proporcionado.
> 2.  **Identificar Artefactos Clave:** Presta mucha atención a los archivos de configuración (`package.json`, `requirements.txt`, `pom.xml`, `Dockerfile`, `.eslintrc`, `.prettierrc`, etc.), READMEs, jerarquía de carpetas, archivos de documentación y archivos de código fuente.
> 3.  **Incorporar Directrices de Contribución y Desarrollo:** Busca y analiza cualquier archivo relacionado con el desarrollo, las pruebas o las contribuciones (por ejemplo, `CONTRIBUTING.md`, `DEVELOPMENT.md`, `TESTING.md`). Las instrucciones dentro de estas guías son críticas y deben resumirse e incluirse en el resultado final.
> 4.  **Inferir Estándares:** No te limites a enumerar archivos. Debes inferir los estándares implícitos y explícitos del proyecto a partir de su estructura y código.
>
> Genera un único archivo Markdown bien formateado llamado `GEMINI.md`. El contenido de este archivo debe estructurarse de acuerdo con la siguiente plantilla. Rellena cada sección basándote en tu análisis. Si no puedes determinar con confianza la información para una sección, indica que es inferida y nota tu nivel de confianza, o sugiérelo como un área para que el desarrollador humano la complete.

---

## Estructura del Archivo a Generar (Plantilla)

Esta es la plantilla que la IA debe rellenar:

```markdown
# GEMINI.MD: Guía de Colaboración con IA

Este documento proporciona contexto esencial para los modelos de IA que interactúan con este proyecto. Adherirse a estas pautas asegurará la consistencia y mantendrá la calidad del código.

## 1. Descripción General y Propósito del Proyecto

* **Objetivo Principal:** [Analiza el README.md, la documentación y los nombres de las carpetas para inferir y resumir el propósito principal del proyecto y para qué está diseñado. Por ejemplo: "Este es un backend API REST para una aplicación de redes sociales."]
* **Dominio de Negocio:** [Describe el dominio en el que opera el proyecto, por ejemplo, "Comercio electrónico", "Fintech", "Analítica de Salud."]

## 2. Tecnologías Principales y Stack

* **Lenguajes:** [Enumera los lenguajes de programación principales y las versiones específicas detectadas, por ejemplo, "TypeScript", "Python 3.11."]
* **Frameworks y Entornos de Ejecución:** [Enumera los principales frameworks y el entorno de ejecución, por ejemplo, "Node.js v20", "React 18", "Spring Boot 3.0", "Django 4.2."]
* **Bases de Datos:** [Identifica los sistemas de bases de datos utilizados, por ejemplo, "PostgreSQL", "Redis para caché", "MongoDB."]
* **Librerías/Dependencias Clave:** [Enumera las librerías más críticas que definen la funcionalidad del proyecto, por ejemplo, "Pandas", "Express.js", "SQLAlchemy", "Axios."]
* **Gestor(es) de Paquetes:** [Identifica los gestores de paquetes utilizados, por ejemplo, "npm", "pip", "Maven."]

## 3. Patrones Arquitectónicos

* **Arquitectura General:** [Infiere la arquitectura de alto nivel. Indica tu razonamiento. Ejemplos: "Aplicación Monolítica", "Arquitectura de Microservicios", "Modelo-Vista-Controlador (MVC)", "Funciones Serverless."]
* **Filosofía de la Estructura de Directorios:** [Explica el propósito de los directorios principales. Ejemplo:
    * `/src`: Contiene todo el código fuente principal.
    * `/iac`: Contiene Infraestructura como Código (ej. Terraform).
    * `/tests`: Contiene todas las pruebas unitarias y de integración.
    * `/config`: Contiene archivos de entorno y configuración.]

## 4. Convenciones de Codificación y Guía de Estilo

* **Formato:** [Infiere de los archivos fuente y cualquier configuración de linter como `.prettierrc` o `.eslintrc`. Nota cualquier guía de estilo estándar mencionada (ej. PEP 8 para Python). Ejemplo: "Indentación: 2 espacios. Adherirse a la guía de estilo PEP 8."]
* **Convenciones de Nomenclatura:** [Analiza nombres de variables, funciones, clases y archivos. Ejemplo:
    * `variables`, `funciones`: camelCase (`miVariable`)
    * `clases`, `componentes`: PascalCase (`MiClase`)
    * `archivos`: kebab-case (`mi-componente.js`)]
* **Diseño de API:** [Si aplica, describe el estilo de la API. Ejemplo: "Principios RESTful. Los endpoints son sustantivos en plural. Usa verbos HTTP estándar (GET, POST, PUT, DELETE). JSON para cuerpos de solicitud/respuesta."]
* **Manejo de Errores:** [Observa patrones comunes de manejo de errores. Ejemplo: "Usa async/await con bloques try...catch. Las clases de error personalizadas se definen en `/src/errors`."]

## 5. Archivos Clave y Puntos de Entrada

* **Punto(s) de Entrada Principal(es):** [Identifica el punto de inicio de la aplicación, ej. `src/index.js`, `app.py`.]
* **Configuración:** [Enumera los archivos principales para la configuración del entorno y la aplicación, ej. `.env`, `config/application.yml`, `settings.py`.]
* **Pipeline CI/CD:** [Identifica el archivo de configuración de integración continua, ej. `.github/workflows/main.yml`, `.gitlab-ci.yml`.]

## 6. Flujo de Trabajo de Desarrollo y Pruebas

* **Entorno de Desarrollo Local:** [Resume el procedimiento estándar para configurar y ejecutar el proyecto localmente. Nota herramientas o comandos clave (ej. `skaffold dev`, `docker-compose up`).]
* **Pruebas:** [Describe cómo se ejecutan las pruebas. Nota cualquier comando o framework específico. Ejemplo: "Ejecutar pruebas vía `npm test`. El código nuevo requiere pruebas unitarias correspondientes."]
* **Proceso CI/CD:** [Explica brevemente qué sucede cuando se confirma código o se crea un PR, basado en los archivos del pipeline CI/CD.]

## 7. Instrucciones Específicas para la Colaboración con IA

* **Pautas de Contribución:** [Resume instrucciones clave de `CONTRIBUTING.md` o archivos similares. Ejemplo: "Todos los pull requests deben enviarse contra la rama `develop` y requieren una revisión de código. Firmar el CLA."]
* **Infraestructura (IaC):** [Nota si existe un directorio de Infraestructura como Código (ej. `/iac`). Agrega una advertencia. Ejemplo: "Los cambios en archivos en el directorio `/iac` modifican la infraestructura en la nube y deben ser revisados y aprobados cuidadosamente."]
* **Seguridad:** [Agrega un recordatorio general sobre las mejores prácticas de seguridad. Ejemplo: "Ten en cuenta la seguridad. No hardcodees secretos o claves. Asegura que cualquier cambio en la lógica de autenticación (ej. JWTs) sea seguro y verificado."]
* **Dependencias:** [Explica el proceso para agregar nuevas dependencias. Ejemplo: "Al agregar una nueva dependencia, usa `npm install --save-dev` y actualiza el archivo `package.json`."]
* **Mensajes de Commit:** [Si existe un directorio `.git`, analiza el historial de commits para buscar patrones. Ejemplo: "Sigue la especificación de Conventional Commits (ej. `feat:`, `fix:`, `docs:`)."]
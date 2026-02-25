# Gemini CLI: 10 Pro Tips You're Not Using

He estado usando mucho la CLI de Gemini últimamente para mis proyectos de codificación. Realmente me gusta cómo me ayuda a trabajar más rápido directamente en mi terminal. Pero cuando empecé, no siempre obtenía los mejores resultados. Con el tiempo, he aprendido algunos trucos sencillos que marcan una gran diferencia. Si usas la CLI de Gemini, quiero compartir mis 10 mejores consejos profesionales. Si estás listo, ¡empecemos!

## 1. Siempre abre primero la carpeta de tu proyecto

¡Este es un paso súper importante que siempre hago! Antes de ejecutar el comando `gemini`, me aseguro de estar ya dentro de la carpeta de mi proyecto. Esto ayuda a Gemini a obtener la vista correcta de mi código y a cargar el archivo `GEMINI.md` correcto. Me ahorra tiempo y ayuda a mantener privados otros archivos que no forman parte del proyecto.

## 2. Elabora indicaciones claras y específicas

El propio Gemini te dice que seas específico para obtener los mejores resultados, y estoy de acuerdo. Las indicaciones deficientes a menudo fallan porque carecen de contexto.

**Indicación vaga (no recomendada):**
```bash
# Vague prompt (not recommended)
help me fix my UI
```

En su lugar, sé explícito sobre lo que quieres y divide la tarea en pasos. Incluso puedes indicarle a Gemini que espere tu confirmación:

**Indicación mejorada con contexto:**
```text
When I tap on a chat message, save that portion of the UI as an image. Provide TypeScript code to implement this feature. Create a step‑by‑step checklist and ask for my approval before editing any files.
```

## 3. Pide un plan antes de realizar cambios

Si me preocupa usar demasiados tokens (o simplemente quiero ser cuidadoso), le pido a Gemini que "genere el plan" primero. Esto me da una lista clara de lo que va a cambiar. Si veo un error en el plan, puedo pedirle a Gemini que lo corrija antes de que empiece a cambiar mis archivos. ¡Esto ahorra mucho tiempo!

## 4. Proporciona contexto persistente con GEMINI.md

Para darle a la CLI de Gemini conocimientos de fondo sobre tu proyecto, crea una carpeta `.gemini` en tu repositorio y añade un archivo `GEMINI.md`. Utilizo el comando `/init` para crear un `GEMINI.md`. En este archivo, documento todos los aspectos importantes de mi proyecto, como guías de estilo, detalles de la audiencia e instrucciones para ejecutar pruebas. Gemini utiliza esta información desde la primera vez que se inicia.

Si cambio el archivo, simplemente uso el comando `/memory refresh` para actualizar el conocimiento de Gemini sobre el proyecto.

```bash
/memory refresh
# verify it's saved
/memory show
```

## 5. Usa el modo shell para comandos rápidos de terminal

En la sesión interactiva, puedes alternar el modo shell presionando `!`.

```bash
gemini
# inside Gemini CLI
! # enters shell mode
pwd # prints the current directory
ls # lists files
! # exits shell mode (or press Esc)
```

El modo shell ejecuta comandos localmente y alimenta la salida de vuelta al contexto de la conversación.

## 6. Usa `/memory add` para una actualización rápida del contexto

Para notas rápidas, como un número de puerto de base de datos o una URL de API, uso el comando `/memory add`. Esta es una forma rápida de añadir detalles específicos a la memoria de Gemini, y es más rápido que abrir y editar el archivo `GEMINI.md` cada vez.

```bash
# store a decision
/memory add "The database port is 123 and we decided to use Boostrap CSS."
# verify it's saved
/memory show
```

## 7. Busca en la web con `@search`

La herramienta `@search` incorporada obtiene información de la web o de fuentes externas. Por ejemplo, si necesitas investigar un problema conocido en GitHub:

```bash
@search "https://github.com/google-gemini/gemini-cli/"
```

Gemini obtiene el problema y lo usa como contexto para responder a tu pregunta. También puedes buscar por palabra clave:

```bash
@search "How to fix 'Cannot find module' error in Node.js?"
```

O simplemente le digo al asistente que "busque en la web", ¡y es lo suficientemente inteligente como para manejar el resto!

## 8. Comandos slash personalizados

Si a menudo haces el mismo tipo de pregunta, define un comando slash personalizado. Supongamos que regularmente necesitas una plantilla de planificación. Crea un directorio y un archivo TOML:

```bash
# create the commands folder
mkdir .gemini/commands
# create a toml file
touch .gemini/commands/plan.toml
```

Dentro del archivo, añade la descripción y la indicación:

```toml
description = "Generate a concise plan from requirements"
prompt = """
You are a project planner. Based on the following requirements, generate a numbered plan with deliverables, time estimates and testing tasks.
Requirements: {{args}}
"""
```

Ahora puedes usar el comando `/plan` dentro de Gemini:

```bash
/plan "Add user authentication and registration to the TODO app."
```

## 9. Usa el modo no interactivo para preguntas individuales

Cuando necesito una respuesta rápida y no quiero iniciar el modo de chat completo, uso el comando `gemini -p`. Simplemente paso mi pregunta con el comando y obtengo una respuesta única y rápida directamente en mi terminal.

```bash
gemini -p "summarize the main points of gemini.md"
```

## 10. Habilita los puntos de control (¡Mi botón de deshacer!)

Esta es mi característica de seguridad favorita. Habilito los puntos de control en mi archivo `settings.json`. Esto es como un "botón de guardar" o un pequeño commit de Git antes de que Gemini realice cualquier cambio. Si el proyecto se rompe después de un cambio, simplemente puedo usar el comando `/restore` para ver la lista de instantáneas guardadas y volver a una versión anterior y funcional de mis archivos.

## Conclusión

La CLI es una herramienta poderosa, pero estos pequeños pasos son lo que realmente la hacen rápida y confiable para mí. ¡Espero que estos consejos también te ayuden a sacarle el máximo provecho! ¡Saludos! ;)

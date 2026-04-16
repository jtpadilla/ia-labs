package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert;

/**
 * Disciplinas de ingeniería que el {@link EngineeringSelector} asigna a solicitudes técnicas.
 *
 * <p>El dispatcher de nivel 2 en {@link EngineeringRouterImpl} lee el estado
 * {@code "engineering_category"} para decidir a qué ingeniero especializado delegar.
 * {@code UNKNOWN} se usa como valor por defecto cuando la categoría no ha sido escrita aún.
 */
public enum EngineeringSelectorResult {
    SOFTWARE, HARDWARE, CIVIL, MECHANICAL, UNKNOWN
}

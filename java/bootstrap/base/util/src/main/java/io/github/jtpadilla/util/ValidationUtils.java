package io.github.jtpadilla.util;

import java.util.function.Supplier;

/**
 * Clase de utilidad para validaciones comunes.
 */
public final class ValidationUtils {

    // Hacemos el constructor privado para que no se pueda instanciar
    private ValidationUtils() {}

    /**
     * Comprueba que el objeto de referencia no sea nulo; de lo contrario,
     * lanza una excepción proporcionada por el Supplier.
     *
     * @param <T> El tipo del objeto a verificar.
     * @param <E> El tipo de la excepción a lanzar (debe ser una RuntimeException).
     * @param obj La referencia del objeto a verificar.
     * @param exceptionSupplier El Supplier que genera la excepción a lanzar.
     * @return El objeto 'obj' si no es nulo (permite asignaciones encadenadas).
     * @throws E si 'obj' es nulo.
     */
    public static <T, E extends Throwable> T notNull(
            T obj,
            Supplier<? extends E> exceptionSupplier) throws E {

        if (obj == null) {
            // Solo si el objeto es nulo, obtenemos y lanzamos la excepción
            throw exceptionSupplier.get();
        }
        // Si no es nulo, lo devolvemos para poder usarlo en asignaciones:
        // this.miCampo = ValidationUtils.requireNonNullElseThrow(miCampo, ...);
        return obj;
    }

}
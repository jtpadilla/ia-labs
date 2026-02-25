package io.github.jtpadilla.protobuf;

import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

public class ProtoThrowableUtil {

    /**
     * Convierte un objeto Throwable a un Struct de Protobuf.
     *
     * @param throwable La excepción o error a convertir.
     * @return Un Struct que representa el Throwable, o un Struct vacío si el input es null.
     */
    public static Struct toStruct(Throwable throwable) {
        if (throwable == null) {
            // Puedes devolver un Struct vacío o lanzar una excepción si prefieres
            return Struct.newBuilder().build();
        }

        Struct.Builder structBuilder = Struct.newBuilder();

        // 1. Nombre de la clase
        structBuilder.putFields("className", Value.newBuilder()
                .setStringValue(throwable.getClass().getName())
                .build());

        // 2. Mensaje (si existe)
        if (throwable.getMessage() != null) {
            structBuilder.putFields("message", Value.newBuilder()
                    .setStringValue(throwable.getMessage())
                    .build());
        } else {
            structBuilder.putFields("message", Value.newBuilder()
                    .setNullValue(NullValue.NULL_VALUE) // O puedes omitirlo
                    .build());
        }


        // 3. StackTrace
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements != null && stackTraceElements.length > 0) {
            ListValue.Builder stackTraceListBuilder = ListValue.newBuilder();
            for (StackTraceElement element : stackTraceElements) {
                stackTraceListBuilder.addValues(Value.newBuilder()
                        .setStructValue(convertStackTraceElementToStruct(element))
                        .build());
            }
            structBuilder.putFields("stackTrace", Value.newBuilder()
                    .setListValue(stackTraceListBuilder)
                    .build());
        }

        // 4. Causa (recursivo)
        Throwable cause = throwable.getCause();
        if (cause != null) {
            structBuilder.putFields("cause", Value.newBuilder()
                    .setStructValue(toStruct(cause)) // Llamada recursiva
                    .build());
        }

        // 5. Excepciones suprimidas (recursivo)
        Throwable[] suppressed = throwable.getSuppressed();
        if (suppressed != null && suppressed.length > 0) {
            ListValue.Builder suppressedListBuilder = ListValue.newBuilder();
            for (Throwable supp : suppressed) {
                suppressedListBuilder.addValues(Value.newBuilder()
                        .setStructValue(toStruct(supp)) // Llamada recursiva
                        .build());
            }
            structBuilder.putFields("suppressed", Value.newBuilder()
                    .setListValue(suppressedListBuilder)
                    .build());
        }


        return structBuilder.build();
    }

    /**
     * Convierte un StackTraceElement a un Struct de Protobuf.
     *
     * @param element El elemento de la traza de pila.
     * @return Un Struct que representa el StackTraceElement.
     */
    private static Struct convertStackTraceElementToStruct(StackTraceElement element) {
        Struct.Builder elementBuilder = Struct.newBuilder();

        elementBuilder.putFields("className", Value.newBuilder().setStringValue(element.getClassName()).build());
        elementBuilder.putFields("methodName", Value.newBuilder().setStringValue(element.getMethodName()).build());

        if (element.getFileName() != null) {
            elementBuilder.putFields("fileName", Value.newBuilder().setStringValue(element.getFileName()).build());
        } else {
            elementBuilder.putFields("fileName", Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build());
        }


        // Solo incluir si es un número de línea válido
        if (element.getLineNumber() >= 0) {
            elementBuilder.putFields("lineNumber", Value.newBuilder().setNumberValue(element.getLineNumber()).build());
        } else {
            // Podrías poner un valor especial o simplemente omitirlo
            elementBuilder.putFields("lineNumber", Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build());
        }


        elementBuilder.putFields("nativeMethod", Value.newBuilder().setBoolValue(element.isNativeMethod()).build());

        // Información del módulo (Java 9+) - Opcional
        /* Descomentar si se usa Java 9+ y se necesita esta información
        if (element.getModuleName() != null) {
            elementBuilder.putFields("moduleName", Value.newBuilder().setStringValue(element.getModuleName()).build());
        }
        if (element.getModuleVersion() != null) {
            elementBuilder.putFields("moduleVersion", Value.newBuilder().setStringValue(element.getModuleVersion()).build());
        }
        */

        return elementBuilder.build();
    }


}
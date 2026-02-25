package com.speedycontrol.labs.example.genai.study.complexfunctioncalling.function.impl.systeminfo;

import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

public class SystemInfoImpl {

    public static Map<String, Object> execute(Map<String, Object> args) throws FunctionGatewayException {
        return execute(Parameters.create(args)).toMap();
    }

    private static Response execute(Parameters parameters) throws FunctionGatewayException {

        final String infoType = parameters.infoType();
        final Optional<String> format = parameters.format();

        return switch (infoType) {

            case "current_time" -> {
                final LocalDateTime now = LocalDateTime.now();
                yield switch (format.orElse("iso")) {
                    case "iso" ->  new Response(now.toString());
                    case "timestamp" -> new Response(String.valueOf(System.currentTimeMillis()));
                    case "readable" -> new Response(now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                    default -> throw new FunctionGatewayException("SystemInfo.format tipo de formato no soportado");
                };
            }

            case "timezone" -> new Response(TimeZone.getDefault().getDisplayName());

            case "system_stats" -> {
                final Runtime runtime = Runtime.getRuntime();
                final long totalMemory = runtime.totalMemory() / (1024 * 1024);
                final long freeMemory = runtime.freeMemory() / (1024 * 1024);
                final int processors = runtime.availableProcessors();
                yield  new Response(String.format("Estadísticas del sistema:%n" +
                                "- Memoria total: %d MB%n" +
                                "- Memoria libre: %d MB%n" +
                                "- Procesadores disponibles: %d%n",
                        totalMemory, freeMemory, processors)
                );
            }

            default -> throw new FunctionGatewayException("SystemInfo.Impl tipo de informacion no soportado");
        };

    }

}

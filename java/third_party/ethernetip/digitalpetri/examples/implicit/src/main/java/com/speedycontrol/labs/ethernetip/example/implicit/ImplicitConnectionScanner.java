package com.speedycontrol.labs.ethernetip.example.implicit;

import com.digitalpetri.enip.EtherNetIpClientConfig;
import com.digitalpetri.enip.EtherNetIpShared;
import com.digitalpetri.enip.cip.CipClient;
import com.digitalpetri.enip.cip.epath.EPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment;
import com.digitalpetri.enip.cip.epath.PortSegment;

import com.digitalpetri.enip.cip.services.ForwardOpenService; // Clase Service
import com.digitalpetri.enip.cip.structs.ForwardOpenResponse;
import com.digitalpetri.enip.cip.structs.NetworkConnectionParameters; // Estructuras de conexión
import com.digitalpetri.enip.cip.structs.ForwardOpenRequest; // Petición de Contrato

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

// Ver explicacion en //java/third_party/ethernetip/docs/notebooklm/*.txt
public class ImplicitConnectionScanner {

    private static final String ADAPTER_IP = "192.168.1.50";

    public static void main(String[] args) throws Exception {

        // 1. Configuración del Cliente y Ruta de Conexión
        EtherNetIpClientConfig config = EtherNetIpClientConfig.builder(ADAPTER_IP)
                .setTimeout(Duration.ofSeconds(5))
                .build();

        // Ruta de Conexión: Indica cómo alcanzar el procesador/módulo
        EPath.PaddedEPath connectionPath = new EPath.PaddedEPath(
                new PortSegment(1, new byte[]{(byte) 0})
        );

        CipClient client = new CipClient(config, connectionPath);
        client.connect().get();
        System.out.println("Conexión TCP establecida con el Adapter: " + ADAPTER_IP);

        // 2. Definición de los Parámetros de la Conexión Implícita (el Contrato)
        // El RPI (Requested Packet Interval): 20ms
        Duration rpi = Duration.ofMillis(20);

        // En esta versión de NetworkConnectionParameters no hay builder(),
        // se usa el constructor:
        //   new NetworkConnectionParameters(
        //       connectionSize,
        //       SizeType,
        //       Priority,
        //       ConnectionType,
        //       boolean variable)
        //
        // Usamos tamaños de ejemplo: 200 bytes (el mayor de entrada/salida).
        int connectionSize = 200;

        NetworkConnectionParameters ioParams =
                new NetworkConnectionParameters(
                        connectionSize,
                        NetworkConnectionParameters.SizeType.Variable,
                        NetworkConnectionParameters.Priority.Low,
                        NetworkConnectionParameters.ConnectionType.PointToPoint,
                        false);

        // 3. Creación de la Petición Forward_Open
        // Firma usada por la librería:
        //   ForwardOpenRequest(
        //       Duration timeout,
        //       int o2tConnectionId,
        //       int t2oConnectionId,
        //       int connectionSerialNumber,
        //       int vendorId,
        //       long vendorSerialNumber,
        //       int connectionTimeoutMultiplier,
        //       EPath.PaddedEPath connectionPath,
        //       Duration o2tRpi,
        //       NetworkConnectionParameters o2tParameters,
        //       Duration t2oRpi,
        //       NetworkConnectionParameters t2oParameters,
        //       int transportClassAndTrigger)
        //
        Duration timeout = Duration.ofSeconds(15);
        int o2tConnectionId = 0; // 0 = que el adaptador asigne, o valor de ejemplo
        int t2oConnectionId = 1; // valor de ejemplo
        int connectionSerialNumber = new Random().nextInt();

        ForwardOpenRequest request = new ForwardOpenRequest(
                timeout,
                o2tConnectionId,
                t2oConnectionId,
                connectionSerialNumber,
                config.getVendorId(),
                config.getSerialNumber(),
                1, // Connection Timeout Multiplier
                // Connection Manager del Adapter (clase 0x06, instancia 1)
                new EPath.PaddedEPath(
                        new LogicalSegment.ClassId(0x06),
                        new LogicalSegment.InstanceId(1)),
                // O->T (Scanner -> Adapter)
                rpi,
                ioParams,
                // T->O (Adapter -> Scanner)
                rpi,
                ioParams,
                0xA3 // Transport Class & Trigger: Clase 1 cíclica típica
        );

        // 4. Creación del Servicio Explícito Forward_Open
        ForwardOpenService service = new ForwardOpenService(request);

        // 5. Invocación Asíncrona del Servicio Explícito (Fase de Negociación)
        CompletableFuture<ForwardOpenResponse> future = client.invoke(service);

        // 6. Manejo de la Respuesta (Asíncrona)
        ForwardOpenResponse response = future.get(); // Espera el resultado

        /***
         * Error en compilacion...
        if (response.getGeneralStatus() == 0x00) {
            System.out.println("✅ Contrato Forward_Open EXITOSO.");
            System.out.printf("   Se establecieron los IDs de Conexión: O->T (0x%X), T->O (0x%X)%n",
                    response.getO2tConnectionId(),
                    response.getT2oConnectionId());
            System.out.println("   La comunicación I/O (UDP/2222) ha comenzado al RPI de " + rpi.toMillis() + "ms.");

            // En una aplicación real, aquí se almacenarían los IDs de conexión
            // y se gestionaría el flujo de datos UDP/2222.
        } else {
            System.err.printf("❌ Fallo en Forward_Open. Código de Estado: 0x%X%n", response.getGeneralStatus());
        }
        */

        // 7. Desconexión y Liberación de Recursos (CRÍTICO)
        client.disconnect().get();
        EtherNetIpShared.releaseSharedResources();
    }
}
package com.speedycontrol.labs.ethernetip.example.explicit;

import com.digitalpetri.enip.EtherNetIpClientConfig;
import com.digitalpetri.enip.EtherNetIpShared;
import com.digitalpetri.enip.cip.CipClient;
import com.digitalpetri.enip.cip.epath.EPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment;
import com.digitalpetri.enip.cip.epath.PortSegment;
import com.digitalpetri.enip.cip.services.GetAttributeListService;
import com.digitalpetri.enip.cip.structs.AttributeResponse; // <-- NUEVO

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

// Ver explicacion en //java/third_party/ethernetip/docs/notebooklm/*.txt
public class EtherNetIpAdapterQuery {

    // Dirección IP de su dispositivo EtherNet/IP (Adaptador, PLC, etc.)
    private static final String TARGET_IP = "10.20.4.57";

    /**
     * Entry point of the application. This method establishes a connection to an EtherNet/IP
     * device, executes a CIP explicit messaging service to retrieve and process attributes,
     * and then terminates the connection while releasing resources.
     *
     * @param args command-line arguments passed to the program
     * @throws Exception if any error occurs during the execution of the application
     */
    public static void main(String[] args) throws Exception {

        // 1. Configuración del Cliente E/IP
        // Se define la IP y el tiempo máximo de espera.
        EtherNetIpClientConfig config = EtherNetIpClientConfig.builder(TARGET_IP)
                .setSerialNumber(0x00)
                .setVendorId(0x00)
                .setTimeout(Duration.ofSeconds(2)) // 2 segundos de timeout [9]
                .build();

        // 2. Definición de la Ruta de Conexión (Connection Path)
        // Esta ruta le indica a la librería cómo enrutar el mensaje CIP dentro del dispositivo.
        // Ejemplo: Puerto 1 (Backplane) y Slot 0 (Target CPU/Módulo).
        EPath.PaddedEPath connectionPath = new EPath.PaddedEPath(
                new PortSegment(1, new byte[]{(byte) 0})
        );

        // 3. Instanciación del Cliente CIP
        // CipClient se construye sobre la base de EtherNet/IP y maneja la lógica CIP. [7]
        CipClient client = new CipClient(config, connectionPath);

        // 4. Conexión y Bloqueo (solo para demostración secuencial)
        // La operación connect() es asíncrona (CompletableFuture). .get() espera el resultado. [1]
        client.connect().get();
        System.out.println("Conexión establecida con " + TARGET_IP);

        // 5. Definición del Servicio CIP Explícito a invocar
        // GetAttributeListService: Usado para leer múltiples atributos a la vez. [10]

        // Ruta del Objeto de Identidad: Clase 0x01, Instancia 0x01. [4]
        EPath.PaddedEPath identityPath = new EPath.PaddedEPath(
                new LogicalSegment.ClassId(0x01),
                new LogicalSegment.InstanceId(0x01)
        );

    GetAttributeListService service =
        new GetAttributeListService(
            identityPath,
            // IDs de atributos a leer
            // 4: Revisión (2 bytes: major, minor)
            // 2: Tipo de dispositivo (UINT, 2 bytes)
            new int[] {4, 2},
            // Tamaños esperados en bytes para cada atributo
            new int[] {2, 2});

    // 6. Invocación del servicio CIP (Mensajería Explícita sin Conexión)
    // invokeUnconnected envía la solicitud CIP y devuelve un CompletableFuture. [2]
    CompletableFuture<AttributeResponse[]> future = client.invokeUnconnected(service);

    // 7. Manejo asíncrono de la respuesta
    future.whenComplete(
        (attributeResponses, ex) -> {
          if (attributeResponses != null) {
            try {
              System.out.println("\n--- Atributos de Identidad Recibidos ---");

              // El array de respuesta debe contener los valores solicitados (4 y 2)
              AttributeResponse revisionAttr = attributeResponses[0];
              AttributeResponse deviceTypeAttr = attributeResponses[1];

              ByteBuf revisionData = revisionAttr.getData();
              ByteBuf deviceTypeData = deviceTypeAttr.getData();

              // Se lee el formato de Revisión (Major Revision, Minor Revision) [11]
              int major = revisionData.readUnsignedByte();
              int minor = revisionData.readUnsignedByte();
              System.out.printf("Revision del Firmware: v%s.%s%n", major, minor); // [2]

              // Se lee el Tipo de Dispositivo (UINT, 16 bits) [12]
              int deviceType = deviceTypeData.readUnsignedShort();
              System.out.printf("Tipo de Dispositivo (ID 0x02): 0x%X%n", deviceType);

            } catch (Throwable t) {
              t.printStackTrace();
            } finally {
              // ¡Liberación de recursos CRÍTICA! [2, 14]
              Arrays.stream(attributeResponses)
                  .map(AttributeResponse::getData)
                  .forEach(ReferenceCountUtil::release);
            }
          } else {
            // Manejo de excepciones (ej. TimeoutException) [1, 9]
            System.err.println("Fallo en la comunicación CIP:");
            ex.printStackTrace();
          }
        });

    // Esperar a que la operación asíncrona (lectura) termine
    future.get();

    // 8. Desconexión y Liberación de Recursos Compartidos
    client.disconnect().get();
    // Liberar pools de hilos de Netty. Es CRÍTICO en aplicaciones que se repiten o finalizan. [1, 14]
    EtherNetIpShared.releaseSharedResources();
  }

}
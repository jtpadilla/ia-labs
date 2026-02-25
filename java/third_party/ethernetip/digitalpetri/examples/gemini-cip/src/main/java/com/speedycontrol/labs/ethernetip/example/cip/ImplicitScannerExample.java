package com.speedycontrol.labs.ethernetip.example.cip;

import com.digitalpetri.enip.EtherNetIpClientConfig;
import com.digitalpetri.enip.cip.CipClient;
import com.digitalpetri.enip.cip.epath.EPath;
import com.digitalpetri.enip.cip.epath.LogicalSegment;
import com.digitalpetri.enip.cip.epath.PortSegment;
import com.digitalpetri.enip.cip.services.CipService;
import com.digitalpetri.enip.cip.structs.MessageRouterRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ImplicitScannerExample {

    // --- CONFIGURACIÓN DE LA CONEXIÓN I/O ---
    // Ajusta estos IDs según tu dispositivo (Estos son típicos de módulos genéricos)
    private static final int INPUT_ASSEMBLY = 101;  // T->O (Target to Originator)
    private static final int OUTPUT_ASSEMBLY = 100; // O->T (Originator to Target)
    private static final int CONFIG_ASSEMBLY = 102; // Config (A veces necesario)

    private static final int RPI_MICROSECONDS = 50000; // 50ms
    private static final int CONNECTION_SERIAL = 0x1337; // Un ID único para esta sesión

    public static void main(String[] args) {
        // 1. Configuración Cliente
        EtherNetIpClientConfig config = EtherNetIpClientConfig.builder("192.168.1.10") // <--- TU IP
                .setSerialNumber(0x00)
                .setVendorId(0x00)
                .setTimeout(Duration.ofSeconds(5))
                .build();

        // 2. Ruta a la CPU (Backplane 1, Slot 0)
        EPath.PaddedEPath connectionPath = new EPath.PaddedEPath(
                new PortSegment(1, new byte[]{(byte) 0})
        );

        // 3. Instancia CipClient
        CipClient client = new CipClient(config, connectionPath);

        client.connect().thenCompose(v -> {
            System.out.println("1. Conexión TCP establecida. Negociando Forward Open...");

            // PASO A: Enviar Petición Forward Open
            return sendForwardOpen(client);

        }).thenAccept(response -> {
            System.out.println("2. ¡Conexión I/O Aceptada!");
            System.out.println("   -> ID O2T (Envío): " + response.o2tId);
            System.out.println("   -> ID T2O (Recepción): " + response.t2oId);

            // PASO B: Iniciar el bucle UDP (Java puro)
            startUdpLoop("192.168.1.10", response.o2tId, response.t2oId);

        }).exceptionally(ex -> {
            System.err.println("ERROR: Fallo en la negociación: " + ex.getMessage());
            return null;
        });

        // Mantener vivo para el bucle UDP
        try { Thread.sleep(60000); } catch (InterruptedException e) {}
    }

    // --- MÉTODOS CIP ---

    private static CompletableFuture<OpenResponse> sendForwardOpen(CipClient client) {
        // Objeto Connection Manager (Clase 6, Instancia 1)
        EPath.PaddedEPath mgrPath = new EPath.PaddedEPath(
                new LogicalSegment.ClassId(0x06),
                new LogicalSegment.InstanceId(0x01)
        );

        // Construir el Payload complejo del Forward Open
        ByteBuf data = buildForwardOpenPayload();

        // Usamos nuestro Wrapper 'SimpleCipService' (Igual que en el ejemplo anterior)
        CipService<OpenResponse> service = new SimpleCipService<>(
                0x54, // Service Code: Forward Open (Standard)
                mgrPath,
                data,
                (buffer) -> {
                    // Decodificar respuesta: Los bytes 4-7 son O2T ID, 8-11 son T2O ID
                    // (Saltamos cabeceras de respuesta CIP)
                    if (buffer.readableBytes() >= 12) {
                        // Forward Open Success response structure (simplified)
                        // Bytes 0-1: Connection Serial (no lo necesitamos)
                        // Bytes 2-3: Vendor ID
                        // Bytes 4-7: O->T Connection ID (Real ID assigned by target)
                        // Bytes 8-11: T->O Connection ID
                        buffer.skipBytes(4);
                        int o2t = buffer.readIntLE(); // Little Endian
                        int t2o = buffer.readIntLE();
                        return new OpenResponse(o2t, t2o);
                    }
                    throw new RuntimeException("Respuesta Forward Open inválida");
                }
        );

        return client.invoke(service);
    }

    private static ByteBuf buildForwardOpenPayload() {
        ByteBuf data = Unpooled.buffer();

        // --- Forward Open Request Parameters ---
        data.writeByte(10); // Priority/Tick (Time tick = 2^10 ms approx)
        data.writeByte(0);  // Timeout ticks

        data.writeIntLE(0);          // O->T Connection ID (0 = Target elige)
        data.writeIntLE(0x11223344); // T->O Connection ID (El que nosotros esperamos recibir)

        data.writeShortLE(CONNECTION_SERIAL);
        data.writeShortLE(0x0000); // Vendor ID
        data.writeIntLE(0x00000000); // Serial Number

        data.writeByte(0); // Timeout Multiplier
        data.writeMediumLE(0); // Reserved

        data.writeIntLE(RPI_MICROSECONDS); // O->T RPI
        // O->T Params: (Size + Fixed/Variable). Ej: 32 bytes + Variable
        data.writeShortLE(makeConnectionParams(32, true));

        data.writeIntLE(RPI_MICROSECONDS); // T->O RPI
        // T->O Params: (Size + Fixed/Variable)
        data.writeShortLE(makeConnectionParams(32, true));

        data.writeByte(0x01); // Transport Trigger (1 = Cyclic)

        // --- CONNECTION PATH (Aquí definimos los Assemblies) ---
        // Construimos la ruta interna "manualmente" para evitar problemas de Clases Abstractas
        // Ruta: Class 4, Inst Conf, Inst Out, Inst In
        ByteBuf pathBuf = Unpooled.buffer();
        // Segmento: Config Assembly
        pathBuf.writeByte(0x20); pathBuf.writeByte(0x04); // Class 4
        pathBuf.writeByte(0x24); pathBuf.writeByte(CONFIG_ASSEMBLY);
        // Segmento: Output Assembly
        pathBuf.writeByte(0x2C); pathBuf.writeByte(OUTPUT_ASSEMBLY); // 0x2C = Connection Point
        // Segmento: Input Assembly
        pathBuf.writeByte(0x2C); pathBuf.writeByte(INPUT_ASSEMBLY);

        // Añadimos tamaño del path (en palabras de 16 bits) y el path mismo
        data.writeByte(pathBuf.readableBytes() / 2);
        data.writeBytes(pathBuf);

        return data;
    }

    // --- UDP LOOP (JAVA PURO) ---
    private static void startUdpLoop(String ip, int o2tId, int t2oId) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        try {
            // Puerto 2222 es el estándar para I/O Implícito
            DatagramSocket socket = new DatagramSocket(2222);
            InetAddress address = InetAddress.getByName(ip);

            // 1. Hilo de Recepción
            new Thread(() -> {
                byte[] buf = new byte[1024];
                while (!socket.isClosed()) {
                    try {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        socket.receive(p);
                        // Aquí recibirías los datos crudos del PLC
                        // Si quieres parsear, debes mirar la cabecera CIP encapsulada en UDP
                        // System.out.println("UDP Recibido: " + p.getLength() + " bytes");
                    } catch (Exception e) {}
                }
            }).start();

            // 2. Hilo de Envío (Cíclico - RPI)
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    // Construir paquete UDP CIP (Sequence + Payload)
                    // Este es un formato simplificado de encapsulamiento I/O
                    ByteBuf udpBuf = Unpooled.buffer();
                    udpBuf.writeShortLE(2); // Item Count

                    // Item 1: Address Item (Connection ID)
                    udpBuf.writeShortLE(0x8002); // Type: Sequenced Address
                    udpBuf.writeShortLE(4);      // Length
                    udpBuf.writeIntLE(o2tId);    // EL ID QUE NOS DIO EL PLC

                    // Item 2: Data Item (Payload)
                    udpBuf.writeShortLE(0x00B1); // Type: Connected Data Transport
                    byte[] myData = new byte[]{0x01, 0x00}; // Datos a enviar (ej. Start bit)
                    udpBuf.writeShortLE(2 + myData.length); // Header (2) + Data
                    udpBuf.writeShortLE(1); // Sequence Count (debería incrementar)
                    udpBuf.writeBytes(myData);

                    byte[] finalBytes = new byte[udpBuf.readableBytes()];
                    udpBuf.readBytes(finalBytes);

                    DatagramPacket packet = new DatagramPacket(finalBytes, finalBytes.length, address, 2222);
                    socket.send(packet);

                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }, 0, 50, TimeUnit.MILLISECONDS); // 50ms ciclo

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- CLASES AUXILIARES ---

    private static short makeConnectionParams(int size, boolean variable) {
        int val = size;
        if (variable) val |= (1 << 9);
        val |= (1 << 15); // Redundant Owner
        return (short) val;
    }

    static class OpenResponse {
        int o2tId;
        int t2oId;
        public OpenResponse(int o, int t) { this.o2tId = o; this.t2oId = t; }
    }

    // --- EL WRAPPER QUE SOLUCIONA LOS ERRORES (EL MISMO DE ANTES) ---
    public static class SimpleCipService<T> implements CipService<T> {
        private final MessageRouterRequest request;
        private final ResponseDecoder<T> decoder;

        public interface ResponseDecoder<T> { T decode(ByteBuf buffer); }

        public SimpleCipService(int serviceCode, EPath.PaddedEPath path, ByteBuf data, ResponseDecoder<T> decoder) {
            this.request = new MessageRouterRequest(serviceCode, path, data);
            this.decoder = decoder;
        }

        @Override
        public void encodeRequest(ByteBuf buffer) {
            MessageRouterRequest.encode(request, buffer);
        }

        @Override
        public T decodeResponse(ByteBuf buffer) {
            return decoder.decode(buffer);
        }
    }
}
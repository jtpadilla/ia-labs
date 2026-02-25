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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ComplexCipExample {

    public static void main(String[] args) {
        // 1. Configuración
        EtherNetIpClientConfig config = EtherNetIpClientConfig.builder("192.168.1.10")
                .setSerialNumber(0x00)
                .setVendorId(0x00)
                .setTimeout(Duration.ofSeconds(5))
                .build();

        // 2. Ruta a la CPU
        EPath.PaddedEPath connectionPath = new EPath.PaddedEPath(
                new PortSegment(1, new byte[]{(byte) 0})
        );

        // 3. Cliente CIP
        CipClient client = new CipClient(config, connectionPath);

        client.connect().thenCompose(v -> {
            System.out.println("--- Conectado ---");
            return readIdentity(client);

        }).thenCompose(productName -> {
            System.out.println("   -> Producto: " + productName);

            // Escribir 1 byte (0x01)
            return writeAssembly(client, 100, new byte[]{0x01});

        }).thenAccept(success -> {
            System.out.println("--- Fin (Resultado: " + success + ") ---");
        }).exceptionally(ex -> {
            System.err.println("ERROR: " + ex.getMessage());
            return null;
        }).whenComplete((res, ex) -> {
            try { client.disconnect().join(); } catch (Exception e) {}
        });

        try { Thread.sleep(5000); } catch (InterruptedException e) {}
    }

    // --- MÉTODOS DE LÓGICA ---

    private static CompletableFuture<String> readIdentity(CipClient client) {
        // Definimos el path como PaddedEPath explícitamente
        EPath.PaddedEPath path = new EPath.PaddedEPath(
                new LogicalSegment.ClassId(0x01),
                new LogicalSegment.InstanceId(0x01),
                new LogicalSegment.AttributeId(0x07)
        );

        CipService<String> service = new SimpleCipService<>(
                0x0E,
                path, // Pasamos PaddedEPath
                Unpooled.EMPTY_BUFFER,
                (buffer) -> {
                    if (buffer.readableBytes() > 0) {
                        int length = buffer.readUnsignedByte();
                        byte[] data = new byte[length];
                        buffer.readBytes(data);
                        return new String(data);
                    }
                    return "Desconocido";
                }
        );

        return client.invoke(service);
    }

    private static CompletableFuture<Boolean> writeAssembly(CipClient client, int instanceId, byte[] data) {
        EPath.PaddedEPath path = new EPath.PaddedEPath(
                new LogicalSegment.ClassId(0x04),
                new LogicalSegment.InstanceId(instanceId),
                new LogicalSegment.AttributeId(0x03)
        );

        ByteBuf dataBuffer = Unpooled.wrappedBuffer(data);

        CipService<Boolean> service = new SimpleCipService<>(
                0x10,
                path, // Pasamos PaddedEPath
                dataBuffer,
                (buffer) -> true
        );

        return client.invoke(service);
    }

    // --- CLASE CORREGIDA (SOLUCIÓN DEL ERROR) ---
    public static class SimpleCipService<T> implements CipService<T> {
        private final MessageRouterRequest request;
        private final ResponseDecoder<T> decoder;

        public interface ResponseDecoder<T> {
            T decode(ByteBuf buffer);
        }

        // CAMBIO CRÍTICO AQUÍ:
        // Antes: SimpleCipService(int serviceCode, EPath path, ...)
        // Ahora: SimpleCipService(int serviceCode, EPath.PaddedEPath path, ...)
        // El constructor debe recibir explícitamente PaddedEPath para satisfacer a MessageRouterRequest.
        public SimpleCipService(int serviceCode, EPath.PaddedEPath path, ByteBuf data, ResponseDecoder<T> decoder) {

            // Ahora esto compila porque 'path' es del tipo exacto PaddedEPath
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
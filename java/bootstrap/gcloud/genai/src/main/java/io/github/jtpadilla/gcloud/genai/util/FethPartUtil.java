package io.github.jtpadilla.gcloud.genai.util;

import com.google.genai.types.Part;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;

public class FethPartUtil {

    final static private int CONNECT_TIMEOUT_MILLIS = 10_000;
    final static private int READ_TIMEOUT_MILLIS = 30_000;

    static final String MIME_TYPE_PDF = "application/pdf";

    public static Part fetchPdfPart(String pdfUrl) throws FethPartException {
        return Part.fromBytes(readBytes(createConnection(pdfUrl)), MIME_TYPE_PDF);
    }

    private static CompletableFuture<Part> fetchPdfPartAsync(String pdfUrl) {
        return CompletableFuture.supplyAsync(()-> {
            try {
                return fetchPdfPart(pdfUrl);
            } catch (FethPartException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static URLConnection createConnection(String urlString) throws FethPartException {
        try {
            // Se abre la conexion
            final URL url = new URI(urlString).toURL();
            final URLConnection connection = url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            return connection;
        } catch (URISyntaxException e) {
            throw new FethPartException("URl invalid", e);
        } catch (IOException e) {
            throw new FethPartException("Input/Output error while trying conection", e);
        }
    }

    private static byte[] readBytes(URLConnection connection) throws FethPartException {
        try (InputStream inputStream = connection.getInputStream()) {

            // Se obtienen todos los byts desde la conexion
            final byte[] bytes = inputStream.readAllBytes();

            if (bytes.length > 0) {
                return bytes;

            } else {
                throw new FethPartException("Empty resource");
            }

        } catch (IOException e) {
            throw new FethPartException("Input/Output error while reading bytes", e);
        }

    }

}

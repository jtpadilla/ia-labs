package io.github.jtpadilla.example.genai.study.fileoperations;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.File;
import com.google.genai.types.UploadFileConfig;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.gcloud.genai.IGenAIService;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Parece que estas llamadas no funcionan en el modo Vertex
public class FileOperations {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            for (String filePath : filesInDirectory("/tmp/examplefiles")) {
                final UploadFileConfig config = UploadFileConfig.builder().mimeType("text/java").build();
                final File file = client.files.upload(filePath, config);
                System.out.println("Uploaded file: " + file);
            }

            // Get the uploaded file.
//            File retrievedFile = client.files.get(file.name().get(), null);
//            System.out.println("Retrieved file: " + retrievedFile);

            // List all files.
//            System.out.println("List files: ");
//            for (File f : client.files.list(ListFilesConfig.builder().pageSize(10).build())) {
//                System.out.println("File name: " + f.name().get());
//            }

            // Delete the uploaded file.
//            DeleteFileResponse unused = client.files.delete(file.name().get(), null);
//            System.out.println("Deleted file: " + file.name().get());

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

    public static java.util.List<String> filesInDirectory(String rutaDirectorio) {
        java.util.List<String> archivosValidos = new java.util.ArrayList<>();

        try {
            Path directorio = Paths.get(rutaDirectorio);

            // Verificar que el directorio existe y es un directorio
            if (!Files.exists(directorio)) {
                System.out.println("El directorio no existe: " + rutaDirectorio);
                return archivosValidos;
            }

            if (!Files.isDirectory(directorio)) {
                System.out.println("La ruta no es un directorio: " + rutaDirectorio);
                return archivosValidos;
            }

            // Bucle que recorre todos los archivos en el directorio
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directorio)) {
                for (Path archivo : stream) {
                    // Solo agregar si es un archivo (no un directorio)
                    if (Files.isRegularFile(archivo)) {
                        archivosValidos.add(archivo.toAbsolutePath().toString());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error al acceder al directorio: " + e.getMessage());
        }

        return archivosValidos;
    }
}

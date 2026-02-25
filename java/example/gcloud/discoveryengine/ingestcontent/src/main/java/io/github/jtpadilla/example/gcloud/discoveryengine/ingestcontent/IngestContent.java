package io.github.jtpadilla.example.gcloud.discoveryengine.ingestcontent;

import com.google.cloud.discoveryengine.v1.CreateDocumentRequest;
import com.google.cloud.discoveryengine.v1.Document;
import com.google.cloud.discoveryengine.v1.DocumentServiceClient;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import com.speedycontrol.labs.example.gcloud.discoveryengine.domain.MyProto;

import java.io.IOException;

public class IngestContent {

    public static void main(String[] args) throws IOException {

        // Reemplaza con tu ID de proyecto, ubicación y ID del Data Store
        String projectId = "tu-proyecto-id";
        String location = "tu-ubicacion"; // Por ejemplo, "us-central1"
        String dataStoreId = "tu-data-store-id";

        // Crea una instancia de tu mensaje Protobuf (asegúrate de haber generado las clases desde tu .proto)
        MyProto myProto = MyProto.newBuilder()
                .setId(1)
                .setName("Ejemplo de Nombre")
                .setDescription("Una descripción detallada del mensaje.")
                .setValue(123.45f)
                .build();

        ingestProtobufData(projectId, location, dataStoreId, myProto);
    }

    public static void ingestProtobufData(String projectId, String location, String dataStoreId, MyProto myProto) throws IOException {

        try (DocumentServiceClient documentServiceClient = DocumentServiceClient.create()) {

            final String parent = String.format(
                    "projects/%s/locations/%s/dataStores/%s/branches/default_branch",
                    projectId,
                    location,
                    dataStoreId
            );

            // Convierte el mensaje Protobuf a un mensaje de tipo struct (protobuf->json->struct)
            final Struct struct = messageToStruct(myProto);

            // Crea el documento para Vertex AI Search
            final Document document = Document.newBuilder()
                    .setDerivedStructData(struct) // Usa mergeFromJson para parsear el String JSON
                    .build();

            // Crea la solicitud para la API
            final CreateDocumentRequest request = CreateDocumentRequest.newBuilder()
                    .setParent(parent)
                    .setDocument(document)
                    .setDocumentId("unique_id_" + myProto.getId()) // Genera un ID único para el documento
                    .build();

            // Envía la solicitud para crear el documento
            final Document response = documentServiceClient.createDocument(request);
            System.out.println("Documento creado: " + response.getName());

        } catch (InvalidProtocolBufferException e) {
            System.err.println("Error al convertir el mensaje Protobuf a JSON: " + e);
        }
    }

    public static Struct messageToStruct(Message message) throws InvalidProtocolBufferException {

        // Usa JsonFormat para convertir el mensaje a una representación JSON
        final String jsonString = JsonFormat.printer().print(message);

        // Usa JsonFormat para convertir la cadena JSON de nuevo a un Struct
        final Struct.Builder structBuilder = Struct.newBuilder();
        JsonFormat.parser().merge(jsonString, structBuilder);
        return structBuilder.build();

    }

}
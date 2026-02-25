package com.speedycontrol.labs.example.vertextai.generativeai.example04;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseStream;
import com.speedycontrol.labs.example.vertextai.common.VertextaiUtil;
import io.github.jtpadilla.protobuf.ProtoJsonUtil;

import java.io.InputStream;

// Este ejemplo muestra como adjuntar una imagen en el promopt
public class GenerateTextFromMultimodalInput {

    static final private String MODEL_NAME = "gemini-2.0-flash-vision";

    private static final String IMAGE_RESOURCE = "/que_sera.jpg";

    public static void main(String[] args) throws Exception {

        try (VertexAI vertexAi = VertextaiUtil.vertexBuilder().build()) {

            // Vision model must be used for multi-modal input
            final GenerativeModel model = new GenerativeModel(MODEL_NAME, vertexAi);

            final Content content = ContentMaker.fromMultiModalData(
                    "Please describe this image",
                    PartMaker.fromMimeTypeAndData("image/jpeg", readImageBytes())
            );

            final ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream(content);

            for (GenerateContentResponse response : responseStream) {
                System.out.println("-------------------------------------");
                System.out.println(ProtoJsonUtil.toJson(response));
            }

        }

    }

    static byte[] readImageBytes() throws Exception {
        try (InputStream inputStream = GenerateTextFromMultimodalInput.class.getResourceAsStream(IMAGE_RESOURCE)) {
            if (inputStream == null) {
                throw new Exception("El recurso no pudo ser encontrado.");
            }
            return inputStream.readAllBytes();
        }
    }

}



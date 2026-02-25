package io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.formatconverter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.time.LocalDateTime;
import java.util.Map;

public class FormatConverterImpl {

    private static final Gson gson = new Gson();

    public static Map<String, Object> execute(Map<String, Object> args) throws FunctionGatewayException {
        return execute(Parameters.create(args)).toMap();
    }

    public static Response execute(Parameters parameters) throws FunctionGatewayException {

        final String data = parameters.data();
        final String targetFormat = parameters.targetFormat();
        final Boolean includeMetadata = parameters.includeMetadata();

        switch (targetFormat) {
            case "json":
                JsonObject jsonResult = new JsonObject();
                jsonResult.addProperty("data", data);
                if (includeMetadata) {
                    jsonResult.addProperty("converted_at", LocalDateTime.now().toString());
                    jsonResult.addProperty("format", "json");
                }
                return new Response(gson.toJson(jsonResult));

            case "xml":
                StringBuilder xml = new StringBuilder();
                xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                xml.append("<result>\n");
                xml.append("  <data>").append(data).append("</data>\n");
                if (includeMetadata) {
                    xml.append("  <converted_at>").append(LocalDateTime.now()).append("</converted_at>\n");
                    xml.append("  <format>xml</format>\n");
                }
                xml.append("</result>");
                return new Response(xml.toString());

            case "csv":
                StringBuilder csv = new StringBuilder();
                csv.append("data");
                if (includeMetadata) {
                    csv.append(",converted_at,format");
                }
                csv.append("\n");
                csv.append("\"").append(data).append("\"");
                if (includeMetadata) {
                    csv.append(",\"").append(LocalDateTime.now()).append("\",csv");
                }
                return new Response(csv.toString());

            default:
                throw new FunctionGatewayException("FormatConverterImpl.Impl.target_format not soported: " + targetFormat);
        }

    }

}

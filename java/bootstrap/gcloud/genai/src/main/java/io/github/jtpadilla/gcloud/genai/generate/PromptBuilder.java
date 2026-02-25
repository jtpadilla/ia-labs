package io.github.jtpadilla.gcloud.genai.generate;

import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import io.github.jtpadilla.gcloud.genai.schema.SchemaEnabled;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PromptBuilder {

    static public PromptBuilder builder() {
        return new PromptBuilder();
    }

    final private List<Part> result;
    private String role;

    public PromptBuilder() {
        this.result =  new ArrayList<>();
        this.role = null;
    }

    public PromptBuilder setRule(String role) {
        this.role = role;
        return this;
    }

    public PromptBuilder addParagraph(String text) {
        result.add(Part.fromText(String.format("%s\n\n", text)));
        return this;
    }

    public PromptBuilder addTitle(String text) {
        result.add(Part.fromText(String.format("%s:\n", text)));
        return this;
    }

    public PromptBuilder addSchemaDefinition(Schema schema) {
        final Supplier<String> schemaSupplier = ()-> {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("```json\n");
            stringBuilder.append(schema.toJson());
            stringBuilder.append("\n```\n\n"); // Doble salto de línea para separar bloques
            return stringBuilder.toString();
        };
        result.add(Part.fromText(schemaSupplier.get()));
        return this;
    }

    public PromptBuilder addSchemaDataList(List<SchemaEnabled> dataList) {
        final List<Part> resultList = new ArrayList<>();
        for (SchemaEnabled dataEntry : dataList) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("```json\n");
            stringBuilder.append(dataEntry.toJson());
            stringBuilder.append("\n```\n\n"); // Doble salto de línea para separar bloques
            resultList.add(Part.fromText(stringBuilder.toString()));
        }
        result.addAll(resultList);
        return this;
    }

    public PromptBuilder addSchemaData(SchemaEnabled data) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("```json\n");
        stringBuilder.append(data.toJson());
        stringBuilder.append("\n```\n\n"); // Doble salto de línea para separar bloques
        result.add(Part.fromText(stringBuilder.toString()));
        return this;
    }

    public Content build() {
        return Content.builder()
                .parts(result)
                .role(role == null ? "user" : role)
                .build();
    }

}

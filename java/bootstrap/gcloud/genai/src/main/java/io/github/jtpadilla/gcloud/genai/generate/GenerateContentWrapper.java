package io.github.jtpadilla.gcloud.genai.generate;

import com.google.genai.types.*;

import java.util.List;
import java.util.Optional;

public class GenerateContentWrapper {

    static public GenerateContentWrapper create(GenerateContentResponse response) throws GenerateContentException {

        final List<Candidate> candidateList = response.candidates().orElseThrow(
                () -> new GenerateContentException(response.getClass().getCanonicalName() + " dont have candidates")
        );

        final Candidate candidate = switch (candidateList.size()) {
            case 0 -> throw new GenerateContentException(response.getClass().getCanonicalName() + " candidates count is 0");
            case 1 -> candidateList.getFirst();
            default -> throw new GenerateContentException(response.getClass().getCanonicalName() + " too many candidates");
        };

        final Content content = candidate.content().orElseThrow(
                () -> new GenerateContentException(candidate.getClass().getCanonicalName() + " dont have content")
        );

        final List<Part> partList = content.parts().orElseThrow(
                () -> new GenerateContentException(content.getClass().getCanonicalName() + " dont have parts")
        );

        if (partList.isEmpty()) {
            throw new GenerateContentException(partList.getClass().getCanonicalName() + " parts count is 0");
        }

        return new GenerateContentWrapper(partList, content.text(), response.usageMetadata().orElse(null));

    }

    final List<Part> parts;
    final String text;
    final GenerateContentResponseUsageMetadata usageMetadata;

    public GenerateContentWrapper(List<Part> parts, String text, GenerateContentResponseUsageMetadata usageMetadata) {
        this.parts = parts;
        this.text = text;
        this.usageMetadata = usageMetadata;
    }

    public List<Part> parts() {
        return parts;
    }

    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    public Optional<GenerateContentResponseUsageMetadata> usage() {
        return Optional.ofNullable(usageMetadata);
    }

    @Override
    public String toString() {
        return "GenerateContentWrapper{" +
                "parts=" + parts +
                ", text='" + text + '\'' +
                ", usageMetadata=" + usageMetadata +
                '}';
    }

}

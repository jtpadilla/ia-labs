package io.github.jtpadilla.gcloud.genai.function.part;

import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract sealed class PartWrapper permits PartTextWrapper, PartFunctionCallWrapper, PartOtherWrapper {
    
    public record Sumary(
            GenerateContentResponse response,
            Content content,
            List<PartFunctionCallWrapper> functionCallList,
            List<PartTextWrapper> textList,
            List<PartOtherWrapper> otherList) {
    }

    static public Sumary extractSumary(GenerateContentResponse response) throws FunctionGatewayException {
        final Content content = extractContent(response);
        final List<PartWrapper> list = extractParts(content);
        return new Sumary(
                response,
                content,
                list.stream().filter(PartFunctionCallWrapper.class::isInstance).map(PartFunctionCallWrapper.class::cast).toList(),
                list.stream().filter(PartTextWrapper.class::isInstance).map(PartTextWrapper.class::cast).toList(),
                list.stream().filter(PartOtherWrapper.class::isInstance).map(PartOtherWrapper.class::cast).toList()
        );
    }

    static public Content extractContent(GenerateContentResponse response) throws FunctionGatewayException {

        final Optional<List<Candidate>> optionalCandidateList = response.candidates();

        // Tiene que tener la lista informada
        if (optionalCandidateList.isEmpty()) {
            throw new FunctionGatewayException("GenerateContentResponse with missing candidate list");
        }
        final List<Candidate> candidateList = optionalCandidateList.get();

        // Unicamente deberia de tener una entrada (hay que configurar la peticion asi)
        final Candidate candidate = switch (candidateList.size()) {
            case 1 -> candidateList.getFirst();
            case 0 -> throw new FunctionGatewayException("GenerateContentResponse with empty candidate list");
            default -> throw new FunctionGatewayException("GenerateContentResponse with to many candidates");
        };

        // Se extrae el content
        if (candidate.content().isEmpty()) {
            throw new FunctionGatewayException("GenerateContentResponse has one candidate without any content");
        }
        return candidate.content().get();

    }

    static public List<PartWrapper> extractParts(Content content) throws FunctionGatewayException {

        // Se confecciona la lista de Parts
        if (content.parts().isEmpty()) {
            throw new FunctionGatewayException("GenerateContentResponse has one candidate with content but without parts");
        }
        final List<Part> parts = content.parts().get();
        final List<PartWrapper> result = new ArrayList<>();
        for (Part part : parts) {
            if (part.text().isPresent()) {
                result.add(new PartTextWrapper(part, part.text().get()));
            } else if (part.functionCall().isPresent()) {
                result.add(new PartFunctionCallWrapper(part, part.functionCall().get()));
            } else {
                result.add(new PartOtherWrapper(part));
            }
        }
        return result;

    }

    final private Part part;

    protected PartWrapper(Part part) {
        this.part = part;
    }

    public Part part() {
        return part;
    }

}

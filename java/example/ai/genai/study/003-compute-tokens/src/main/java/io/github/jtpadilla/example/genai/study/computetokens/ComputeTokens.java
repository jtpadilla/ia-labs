package io.github.jtpadilla.example.genai.study.computetokens;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.ComputeTokensConfig;
import com.google.genai.types.ComputeTokensResponse;
import com.google.genai.types.TokensInfo;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import io.github.jtpadilla.util.BytesUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ComputeTokens {

    public static void main(String[] args) {
        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();
        try (Client client = genAIService.createClient()) {
            final ComputeTokensResponse response = client.models.computeTokens(
                    genAIService.getLlmModel(),
                    "What is your name?",
                    ComputeTokensConfig.builder().build()
            );
            displayResponse(response);
        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }
    }

    private static void displayResponse(ComputeTokensResponse response) {
        final Optional<List<TokensInfo>> optionalTokensInfoList = response.tokensInfo();
        if (optionalTokensInfoList.isEmpty()) {
            System.out.println("La respuesta no contiene datos");
        } else {
            final List<TokensInfo> tokensInfoList = optionalTokensInfoList.get();
            int counter = 0;
            for (TokensInfo tokensInfo : tokensInfoList) {
                System.out.format("TokensInfo %d/%d%n", 1 + counter++, tokensInfoList.size());
                displayTokenInfo(tokensInfo);
                System.out.println();
            }
        }
    }

    private static void displayTokenInfo(TokensInfo tokensInfo) {

        System.out.println("-----------------------------------------");

        // Role
        System.out.format("Role: %s%n", tokensInfo.role().isPresent() ? tokensInfo.role().get() : "EMPTY");

        // TokensIds
        System.out.format("TokensIds: ");
        if (tokensInfo.tokenIds().isPresent()) {
            final String tokenIds = tokensInfo.tokenIds().get().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            System.out.print(tokenIds);
        }
        System.out.println();

        // Tokens
        System.out.format("Tokens (bytes): ");
        if (tokensInfo.tokens().isPresent()) {
            final String tokens = tokensInfo.tokens().get().stream()
                    .map(BytesUtils::toHex)
                    .collect(Collectors.joining(", "));
            System.out.print(tokens);
        }
        System.out.println();

        System.out.format("Tokens (chars): ");
        if (tokensInfo.tokens().isPresent()) {
            final String tokens = tokensInfo.tokens().get().stream()
                    .map(String::new)
                    .collect(Collectors.joining(", "));
            System.out.print(tokens);
        }
        System.out.println();

        System.out.println();
    }

}

package io.github.jtpadilla.example.genai.study.complexfunctioncalling.function.impl.informationsearcher;

import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InformationSearcherImpl {

    public static Map<String, Object> execute(Map<String, Object> args) throws FunctionGatewayException {
        return execute(Parameters.create(args)).toMap();
    }

    public static Response execute(Parameters parameters) {

        final String query = parameters.query();
        final String category = parameters.category();
        final int maxResults = parameters.maxResults();

        // Base de conocimientos simulada
        final Map<String, List<String>> knowledgeBase = Map.of(
                "algorithms", Arrays.asList(
                        "El algoritmo factorial es fundamental en matemáticas y programación",
                        "La complejidad temporal del factorial recursivo es O(n)",
                        "Existen optimizaciones como memorización para mejorar el rendimiento"
                ),
                "mathematics", Arrays.asList(
                        "El factorial de n (n!) es el producto de todos los enteros positivos menores o iguales a n",
                        "0! = 1 por definición matemática",
                        "Los factoriales crecen muy rápidamente (función superexponencial)"
                ),
                "programming", Arrays.asList(
                        "El factorial puede implementarse recursiva o iterativamente",
                        "En Java, hay que considerar el overflow para números grandes",
                        "BigInteger es útil para factoriales de números muy grandes"
                )
        );

        final List<String> results = knowledgeBase.getOrDefault(category, Arrays.asList(
                "Información general sobre: " + query,
                "Esta es una consulta de propósito general",
                "Se recomienda especificar una categoría para mejores resultados"
        ));

        // Limitar resultados según maxResults
        final List<String> limitedResults = results.subList(0, Math.min(maxResults, results.size()));

        final StringBuilder response = new StringBuilder();
        response.append("🔍 Resultados de búsqueda para '").append(query).append("' en categoría '").append(category).append("':\n\n");

        for (int i = 0; i < limitedResults.size(); i++) {
            response.append(String.format("%d. %s\n", i + 1, limitedResults.get(i)));
        }

        return new Response(response.toString());

    }

}

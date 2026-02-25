package com.speedycontrol.labs.example.genai.study.complexfunctioncalling.function.impl.informationsearcher;

import io.github.jtpadilla.gcloud.genai.function.FunctionGatewayException;

import java.util.Map;
import java.util.Optional;

public class Parameters {

    static public Parameters create(Map<String, Object> args) throws FunctionGatewayException {
        final Builder builder = builder(createQuery(args));
        createCategory(args).ifPresent(builder::setCategory);
        createMaxResults(args).ifPresent(builder::setMaxResults);
        return builder.build();
    }

    static private String createQuery(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("query"));
        if (object.isEmpty()) {
            throw new FunctionGatewayException("InformationSearcher.query is null");
        }
        if (object.get() instanceof String) {
            return (String) object.get();
        } else {
            throw new FunctionGatewayException("InformationSearcher.query not is String");
        }
    }

    static private Optional<String> createCategory(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("category"));
        if (object.isEmpty()) {
            return Optional.empty();
        }
        if (object.get() instanceof String) {
            return Optional.of(((String) object.get()));
        } else {
            throw new FunctionGatewayException("InformationSearcher.category not is String");
        }
    }

    static private Optional<Integer> createMaxResults(Map<String, Object> args) throws FunctionGatewayException {
        final Optional<Object> object = Optional.ofNullable(args.get("max_results"));
        if (object.isEmpty()) {
            return Optional.empty();
        }
        if (object.get() instanceof Number) {
            return Optional.of(((Number) object.get()).intValue());
        } else {
            throw new FunctionGatewayException("InformationSearcher.maxResults not is Number");
        }
    }

    static private Builder builder(String query) {
        return new Builder(query);
    }

    static private class Builder {

        final private String query;
        private String category;
        private Integer maxResults;

        private Builder(String query) {
            this.query = query;
            this.category = null;
            this.maxResults = null;
        }

        public Builder setCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder setMaxResults(Integer results) {
            this.maxResults = results;
            return this;
        }

        public Parameters build() {
            return new Parameters(
                    query,
                    category == null ? "general" : category,
                    maxResults == null ? 3 : maxResults
            );
        }

    }

    final private String query;
    final private String category;
    final private int maxResults;

    private Parameters(String query, String category, Integer maxResults) {
        this.query = query;
        this.category = category;
        this.maxResults = maxResults;
    }

    public String query() {
        return query;
    }

    public String category() {
        return category;
    }

    public int maxResults() {
        return maxResults;
    }

}

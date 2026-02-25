package io.github.jtpadilla.gcloud.context.internal;

import io.github.jtpadilla.gcloud.context.UnitResource;
import io.github.jtpadilla.unit.parameter.IParameterDescriptor;
import io.github.jtpadilla.unit.parameter.IParameterReader;
import io.github.jtpadilla.unit.parameter.IParameterReaderBuilder;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

class GCloudContextConfig {

	static final private IParameterDescriptor GCLOUD_PROJECT_ID = IParameterDescriptor.createStringParameterDescriptor(
			"GCLOUD_PROJECT_ID",
			"Identificador del proyecto GCLOUD"
	);

    static final private IParameterDescriptor GCLOUD_CREDENTIALS = IParameterDescriptor.createStringParameterDefaultDescriptor(
            "GCLOUD_CREDENTIALS",
            "Ruta absoluta del archivo de credenciales GCLOUD",
            "DEFAULT"
    );

    static final private IParameterDescriptor GENAI_MODEL_LLM = IParameterDescriptor.createStringParameterDefaultDescriptor(
            "GENAI_MODEL_LLM",
            "Nombre del Modelo LLM GenAI en el Endpoint Vertex",
            "gemini-2.5-flash-lite"
    );

    static final private IParameterDescriptor GENAI_MODEL_EMBED = IParameterDescriptor.createStringParameterDefaultDescriptor(
            "GENAI_MODEL_EMBED",
            "Nombre del Modelo Embed GenAI en el Endpoint Vertex",
            "gemini-embedding-001"
    );

    static final private IParameterDescriptor GENAI_LOCATION = IParameterDescriptor.createStringParameterDefaultDescriptor(
            "GENAI_LOCATION",
            "Localizacion del Modelo GenAI en el Endpoint Vertex",
            "global"
    );

    static final private IParameterDescriptor GENAI_CLIENT_MAX_CONNECTIONS = IParameterDescriptor.createIntegerParameterOptionalDescriptor(
            "GENAI_CLIENT_MAX_CONNECTIONS",
            "Numero maximo de conexiones permitidas en el pool",
            1,
            512
    );

    static final private IParameterDescriptor GENAI_CLIENT_MAX_CONNECTIONS_PER_HOST = IParameterDescriptor.createIntegerParameterOptionalDescriptor(
            "GENAI_CLIENT_MAX_CONNECTIONS_PERHOST",
            "Numero maximo de conexiones permitidas por host",
            1,
            512
    );

    static final private IParameterDescriptor GENAI_HTTP_TIMEOUT = IParameterDescriptor.createIntegerParameterOptionalDescriptor(
            "GENAI_HTTP_TIMEOUT",
            "Timeout por peticion en milisegundos",
            1000,
            20000
    );

    static final private IParameterDescriptor GENAI_HTTP_BASEURL = IParameterDescriptor.createStringParameterOptionalDescriptor(
            "GENAI_HTTP_BASEURL",
            "Url base para en endpoint del servicio AI de la plataforma "
    );

    static GCloudContextConfig getConfig() {
        return new GCloudContextConfig();
    }

    final private String gcloudProject;
    final private String gcloudCredentials;

    final private String genAIModelLlm;
    final private String genAIModelEmbed;
    final private String genAILocation;

    @Nullable
    final private Integer genAIClientMaxConnections;
    @Nullable
    final private Integer genAIClientMaxConnectionsPerHost;
    @Nullable
    final private Integer genAIHttpTimeout;
    @Nullable
    final private String genAIHttpBaseurl;

    private GCloudContextConfig() {
        final IParameterReader reader = IParameterReaderBuilder.create(UnitResource.UNIT)
                .addParameterDescriptor(GCLOUD_PROJECT_ID)
                .addParameterDescriptor(GCLOUD_CREDENTIALS)
                .addParameterDescriptor(GENAI_MODEL_LLM)
                .addParameterDescriptor(GENAI_MODEL_EMBED)
                .addParameterDescriptor(GENAI_LOCATION)
                .addParameterDescriptor(GENAI_CLIENT_MAX_CONNECTIONS)
                .addParameterDescriptor(GENAI_CLIENT_MAX_CONNECTIONS_PER_HOST)
                .addParameterDescriptor(GENAI_HTTP_TIMEOUT)
                .addParameterDescriptor(GENAI_HTTP_BASEURL)
                .build();
        gcloudProject = reader.getValue(GCLOUD_PROJECT_ID).getStringValue();
        gcloudCredentials = reader.getValue(GCLOUD_CREDENTIALS).getStringValue();
        genAIModelLlm = reader.getValue(GENAI_MODEL_LLM).getStringValue();
        genAIModelEmbed = reader.getValue(GENAI_MODEL_EMBED).getStringValue();
        genAILocation = reader.getValue(GENAI_LOCATION).getStringValue();
        genAIClientMaxConnections = reader.getValue(GENAI_CLIENT_MAX_CONNECTIONS).getIntegerOptionalValue().orElse(null);
        genAIClientMaxConnectionsPerHost = reader.getValue(GENAI_CLIENT_MAX_CONNECTIONS_PER_HOST).getIntegerOptionalValue().orElse(null);
        genAIHttpTimeout = reader.getValue(GENAI_HTTP_TIMEOUT).getIntegerOptionalValue().orElse(null);
        genAIHttpBaseurl = reader.getValue(GENAI_HTTP_BASEURL).getStringOptionalValue().orElse(null);
    }

    String getGCloudProjectId() {
        return gcloudProject;
	}

    public String getGcloudCredentials() {
        return gcloudCredentials;
    }

    public String getGenAIModelLlm() {
        return genAIModelLlm;
    }

    public String getGenAIModelEmbed() {
        return genAIModelEmbed;
    }

    public String getGenAILocation() {
        return genAILocation;
    }

    public Optional<Integer> getGenAIClientMaxConnections() {
        return genAIClientMaxConnections == null ? Optional.empty() : Optional.of(genAIClientMaxConnections);
    }

    public Optional<Integer> getGenAIClientMaxConnectionsPerHost() {
        return genAIClientMaxConnectionsPerHost == null ? Optional.empty() : Optional.of(genAIClientMaxConnectionsPerHost);
    }

    public Optional<Integer> getGenAIHttpTimeout() {
        return genAIHttpTimeout == null ? Optional.empty() : Optional.of(genAIHttpTimeout);
    }

    public Optional<String> getGenAIHttpBaseurl() {
        return genAIHttpBaseurl == null ? Optional.empty() : Optional.of(genAIHttpBaseurl);
    }

}

package io.github.jtpadilla.gcloud.context;

import com.google.auth.oauth2.GoogleCredentials;

import java.util.Optional;

public interface IGCloudContextService {

	String getGCloudProjectId();
	GoogleCredentials getGcloudCredentials();

	String getGenAILlmModel();
    String getGenAIEmbedModel();
    String getGenAILocation();

    Optional<Integer> getGenAIClientMaxConnections();
    Optional<Integer> getGenIClientMaxConnectionsPerhost();
    Optional<Integer> getGenAIHttpTimeout();
    Optional<String> getGenAIHttpBaseurl();

}

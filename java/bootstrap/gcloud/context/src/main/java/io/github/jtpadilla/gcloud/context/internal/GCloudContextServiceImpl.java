package io.github.jtpadilla.gcloud.context.internal;

import com.google.auth.oauth2.GoogleCredentials;
import io.github.jtpadilla.gcloud.context.IGCloudContextService;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

public class GCloudContextServiceImpl implements IGCloudContextService {

	final private GCloudContextConfig gcloudContextConfig;
	final private GoogleCredentials gcloudCredentials;
	
	public GCloudContextServiceImpl() {

		// Configuración
		gcloudContextConfig = GCloudContextConfig.getConfig();
		
		// Credenciales de google
		try {
            if (gcloudContextConfig.getGcloudCredentials().equalsIgnoreCase("DEFAULT")) {
				gcloudCredentials = GoogleCredentials.getApplicationDefault();
            } else {
                gcloudCredentials = GoogleCredentials.fromStream(new FileInputStream(gcloudContextConfig.getGcloudCredentials()));
            }
		} catch (IOException e) {
			throw new IllegalArgumentException("Imposible obtener las credenciales para acceder a los servicios de google: %s", e);
		}
		
	}
	
	@Override
	public String getGCloudProjectId() {
		return gcloudContextConfig.getGCloudProjectId();
	}

	@Override
	public GoogleCredentials getGcloudCredentials() {
		return gcloudCredentials;
	}

	@Override
	public String getGenAILlmModel() {
		return gcloudContextConfig.getGenAIModelLlm();
	}

    @Override
    public String getGenAIEmbedModel() {
        return gcloudContextConfig.getGenAIModelEmbed();
    }

    @Override
    public String getGenAILocation() {
        return gcloudContextConfig.getGenAILocation();
    }

    @Override
    public Optional<Integer> getGenAIClientMaxConnections() {
        return gcloudContextConfig.getGenAIClientMaxConnections();
    }

    @Override
    public Optional<Integer> getGenIClientMaxConnectionsPerhost() {
        return gcloudContextConfig.getGenAIClientMaxConnectionsPerHost();
    }

    @Override
    public Optional<Integer> getGenAIHttpTimeout() {
        return gcloudContextConfig.getGenAIHttpTimeout();
    }

    @Override
    public Optional<String> getGenAIHttpBaseurl() {
        return gcloudContextConfig.getGenAIHttpBaseurl();
    }
}

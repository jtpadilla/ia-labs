package io.github.jtpadilla.gcloud.context;

import com.google.auth.oauth2.GoogleCredentials;
import io.github.jtpadilla.gcloud.context.internal.GCloudContextServiceImpl;

import java.util.Optional;

public class DefaultGCloudContextService implements IGCloudContextService {

    final private IGCloudContextService delegate;

    public DefaultGCloudContextService() {
        this.delegate = new GCloudContextServiceImpl();
    }

    @Override
    public String getGCloudProjectId() {
        return delegate.getGCloudProjectId();
    }

    @Override
    public GoogleCredentials getGcloudCredentials() {
        return delegate.getGcloudCredentials();
    }

    @Override
    public String getGenAILlmModel() {
        return delegate.getGenAILlmModel();
    }

    @Override
    public String getGenAIEmbedModel() {
        return delegate.getGenAIEmbedModel();
    }

    @Override
    public String getGenAILocation() {
        return delegate.getGenAILocation();
    }

    @Override
    public Optional<Integer> getGenAIClientMaxConnections() {
        return delegate.getGenAIClientMaxConnections();
    }

    @Override
    public Optional<Integer> getGenIClientMaxConnectionsPerhost() {
        return delegate.getGenIClientMaxConnectionsPerhost();
    }

    @Override
    public Optional<Integer> getGenAIHttpTimeout() {
        return delegate.getGenAIHttpTimeout();
    }

    @Override
    public Optional<String> getGenAIHttpBaseurl() {
        return delegate.getGenAIHttpBaseurl();
    }

}

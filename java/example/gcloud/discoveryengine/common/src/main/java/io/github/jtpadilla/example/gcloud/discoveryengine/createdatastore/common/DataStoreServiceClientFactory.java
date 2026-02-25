package io.github.jtpadilla.example.gcloud.discoveryengine.createdatastore.common;

import com.google.cloud.discoveryengine.v1.DataStoreServiceClient;
import com.google.cloud.discoveryengine.v1.DataStoreServiceSettings;
import io.github.jtpadilla.gcloud.context.IGCloudContextService;

import java.io.IOException;

public class DataStoreServiceClientFactory {

    static public DataStoreServiceClient create(IGCloudContextService parameters) throws IOException {
        return DataStoreServiceClient.create(settings(parameters));
    }

    static public DataStoreServiceSettings settings(IGCloudContextService parameters) throws IOException {

        final DataStoreServiceSettings.Builder builder = DataStoreServiceSettings.newBuilder()
                .setCredentialsProvider(parameters::getGcloudCredentials)
                .setQuotaProjectId(parameters.getGCloudProjectId());

        return builder.build();

    }

}

package io.github.jtpadilla.example.gcloud.storage;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.github.jtpadilla.gcloud.context.DefaultGCloudContextService;
import io.github.jtpadilla.gcloud.context.IGCloudContextService;

import java.io.IOException;

public class StorageExample {

    static final IGCloudContextService GCLOUD_CONTEXT_SERVICE = new DefaultGCloudContextService();

    static void main(String[] args) throws IOException {

        final Storage storage = StorageOptions.newBuilder()
                .setProjectId(GCLOUD_CONTEXT_SERVICE.getGCloudProjectId())
                .setCredentials(GCLOUD_CONTEXT_SERVICE.getGcloudCredentials())
                .build()
                .getService();

        System.out.println("Buckets:");
        for (Bucket bucket : storage.list().iterateAll()) {
            System.out.println(bucket.getName());
        }
    }

}
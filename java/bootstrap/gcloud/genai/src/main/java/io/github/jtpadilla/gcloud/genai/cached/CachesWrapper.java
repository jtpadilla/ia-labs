package io.github.jtpadilla.gcloud.genai.cached;

import com.google.genai.Client;
import com.google.genai.types.*;
import io.github.jtpadilla.gcloud.genai.util.FethPartException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CachesWrapper {

    static public CachedContentWrapper create(String model, Client client, Content content, String displayName) throws FethPartException, CachedContentException {

        final CreateCachedContentConfig config =
                CreateCachedContentConfig.builder()
                        .systemInstruction(Content.fromParts(Part.fromText("summarize the pdf")))
                        .expireTime(Instant.now().plus(Duration.ofHours(1)))
                        .contents(content)
                        .displayName(displayName)
                        .build();

        return CachedContentWrapper.create(
                client.caches.create(model, config)
        );

    }

    static public CachedContentWrapper get(Client client, CachedContentName cachedContentName) throws CachedContentException {
        return CachedContentWrapper.create(
            client.caches.get(cachedContentName.name(), null)
        );
    }

    static public CachedContentWrapper update(Client client, CachedContentName cachedContentName, UpdateCachedContentConfig config) throws CachedContentException {
        return CachedContentWrapper.create(
            client.caches.update(cachedContentName.name(), config)
        );
    }

    static public List<CachedContentWrapper> list(Client client, int pageSize) throws CachedContentException {
        final ArrayList<CachedContentWrapper> result = new ArrayList<>();
        for (var entry : client.caches.list(ListCachedContentsConfig.builder().pageSize(pageSize).build())) {
            result.add(CachedContentWrapper.create(entry));
        }
        return result;
    }

    static public void delete(Client client, CachedContentName name) {
        DeleteCachedContentResponse __ = client.caches.delete(name.name(), null);
    }

}

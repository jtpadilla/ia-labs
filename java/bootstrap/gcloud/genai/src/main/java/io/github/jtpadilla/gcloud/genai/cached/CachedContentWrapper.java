package io.github.jtpadilla.gcloud.genai.cached;

import com.google.genai.types.CachedContent;
import com.google.genai.types.CachedContentUsageMetadata;

import java.time.Instant;

public record CachedContentWrapper(
        CachedContentName cachedContentName,
        String model,
        Instant createTime,
        Instant updateTime,
        Instant expireTime,
        CachedContentUsageMetadata usageMetadata) {

    static public CachedContentWrapper create(CachedContent cachedContent) throws CachedContentException {
        return new CachedContentWrapper(
                CachedContentName.create(cachedContent),
                cachedContent.model().orElseThrow(() -> new CachedContentException(cachedContent.getClass().getCanonicalName() + " dont have model")),
                cachedContent.createTime().orElseThrow(() -> new CachedContentException(cachedContent.getClass().getCanonicalName() + " dont have createTime")),
                cachedContent.updateTime().orElseThrow(() -> new CachedContentException(cachedContent.getClass().getCanonicalName() + " dont have updateTime")),
                cachedContent.expireTime().orElseThrow(() -> new CachedContentException(cachedContent.getClass().getCanonicalName() + " dont have expireTime")),
                cachedContent.usageMetadata().orElseThrow(() -> new CachedContentException(cachedContent.getClass().getCanonicalName() + " dont have usageMetadata"))
        );
    }

}

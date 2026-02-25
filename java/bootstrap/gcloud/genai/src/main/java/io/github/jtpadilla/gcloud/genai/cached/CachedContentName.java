package io.github.jtpadilla.gcloud.genai.cached;

import com.google.genai.types.CachedContent;

public record CachedContentName(String name) {

    static public CachedContentName create(CachedContent cachedContent) throws CachedContentException {
        return new CachedContentName(cachedContent.name().orElseThrow(() -> new CachedContentException(cachedContent.getClass().getCanonicalName() + " dont have name")));
    }

}

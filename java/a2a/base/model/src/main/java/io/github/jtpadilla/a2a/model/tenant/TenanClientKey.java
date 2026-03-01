package io.github.jtpadilla.a2a.model.tenant;

import java.util.Objects;

public record TenanClientKey(String id) implements TenantKey {
    public TenanClientKey {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (id.equalsIgnoreCase(TenantDefaultKey.ID)) {
            throw new IllegalArgumentException("Se ha utilizado un ID prohibido.");
        }
    }
}

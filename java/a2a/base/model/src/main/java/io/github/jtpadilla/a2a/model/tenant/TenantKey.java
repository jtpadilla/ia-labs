package io.github.jtpadilla.a2a.model.tenant;

sealed public interface TenantKey permits TenanClientKey, TenantDefaultKey {
    String id();
}

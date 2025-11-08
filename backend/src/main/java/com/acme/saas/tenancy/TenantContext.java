package com.acme.saas.tenancy;

public class TenantContext {
    public static final String DEFAULT_TENANT = "public";
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(String tenant) {
        currentTenant.set(tenant);
    }
    public static String getCurrentTenant() {
        String t = currentTenant.get();
        return t == null ? DEFAULT_TENANT : t;
    }
    public static void clear() {
        currentTenant.remove();
    }
}

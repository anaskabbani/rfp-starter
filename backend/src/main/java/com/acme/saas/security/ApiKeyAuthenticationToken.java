package com.acme.saas.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Authentication token for API key authentication.
 * Used for service-to-service calls.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final String tenantId;

    public ApiKeyAuthenticationToken(String apiKey, String tenantId) {
        super(List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
        this.apiKey = apiKey;
        this.tenantId = tenantId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return "service:" + (tenantId != null ? tenantId : "system");
    }

    public String getTenantId() {
        return tenantId;
    }
}
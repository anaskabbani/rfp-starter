package com.acme.saas.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;

import java.util.Collection;
import java.util.Map;

/**
 * Authentication token for Clerk JWT authentication.
 * Holds the ClerkPrincipal and original JWT token.
 */
public class ClerkAuthenticationToken extends AbstractOAuth2TokenAuthenticationToken<Jwt> {

    private final ClerkPrincipal principal;

    public ClerkAuthenticationToken(ClerkPrincipal principal, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Map<String, Object> getTokenAttributes() {
        return getToken().getClaims();
    }

    @Override
    public ClerkPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.userId();
    }
}
package com.acme.saas.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Converts a Clerk JWT token into a ClerkAuthenticationToken.
 * Extracts organization information from JWT claims.
 */
@Component
public class ClerkJwtAuthenticationConverter implements Converter<Jwt, ClerkAuthenticationToken> {

    @Override
    public ClerkAuthenticationToken convert(Jwt jwt) {
        String userId = jwt.getSubject();
        String orgSlug = jwt.getClaimAsString("org_slug");
        String orgId = jwt.getClaimAsString("org_id");
        String orgRole = jwt.getClaimAsString("org_role");

        if (orgSlug == null || orgSlug.isBlank()) {
            throw new AccessDeniedException("No organization selected. User must be part of an organization.");
        }

        Collection<GrantedAuthority> authorities = extractAuthorities(orgRole);
        ClerkPrincipal principal = new ClerkPrincipal(userId, orgSlug, orgId, orgRole);

        return new ClerkAuthenticationToken(principal, jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(String orgRole) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (orgRole != null && !orgRole.isBlank()) {
            // Convert Clerk role format (e.g., "org:admin") to Spring Security format (e.g., "ROLE_ORG_ADMIN")
            String normalizedRole = orgRole.replace(":", "_").toUpperCase();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
        }

        return authorities;
    }
}
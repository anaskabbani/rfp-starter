package com.acme.saas.security;

import com.acme.saas.service.OrgService;
import com.acme.saas.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that sets the tenant context based on the authenticated principal.
 * Runs after Spring Security authentication to extract tenant from JWT claims
 * or API key header.
 *
 * Also handles lazy provisioning of tenant schemas for new Clerk organizations.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class TenantAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantAuthorizationFilter.class);

    private final OrgService orgService;

    public TenantAuthorizationFilter(OrgService orgService) {
        this.orgService = orgService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()) {
                String tenantId = extractTenantId(auth);

                if (tenantId != null) {
                    TenantContext.setCurrentTenant(tenantId);
                    log.debug("Set tenant context to: {}", tenantId);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractTenantId(Authentication auth) {
        Object principal = auth.getPrincipal();

        if (principal instanceof ClerkPrincipal clerkPrincipal) {
            String orgSlug = clerkPrincipal.orgSlug();
            String tenantId = "tenant_" + orgSlug.toLowerCase();

            // Lazy provisioning: ensure schema exists for this org
            orgService.ensureSchemaExists(orgSlug);

            return tenantId;
        }

        if (auth instanceof ApiKeyAuthenticationToken apiKeyAuth) {
            String tenantHeader = apiKeyAuth.getTenantId();
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                return "tenant_" + tenantHeader.toLowerCase();
            }
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip tenant context for public endpoints
        return path.startsWith("/health")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/api-docs");
    }
}
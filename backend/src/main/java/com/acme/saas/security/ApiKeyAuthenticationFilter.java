package com.acme.saas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Filter for API key authentication.
 * Processes X-API-Key header for service-to-service authentication.
 * Must run before Spring Security's OAuth2 authentication.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final List<String> validApiKeys;

    public ApiKeyAuthenticationFilter(@Value("${api.keys:}") String apiKeys) {
        if (apiKeys == null || apiKeys.isBlank()) {
            this.validApiKeys = Collections.emptyList();
        } else {
            this.validApiKeys = Arrays.asList(apiKeys.split(","));
        }
        log.info("API key authentication configured with {} key(s)", this.validApiKeys.size());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        String tenantId = request.getHeader(TENANT_HEADER);

        if (apiKey != null && !apiKey.isBlank() && validApiKeys.contains(apiKey.trim())) {
            log.debug("Valid API key authentication for tenant: {}", tenantId);
            ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(apiKey, tenantId);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only process if X-API-Key header is present
        return request.getHeader(API_KEY_HEADER) == null;
    }
}
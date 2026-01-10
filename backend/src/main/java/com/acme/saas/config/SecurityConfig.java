package com.acme.saas.config;

import com.acme.saas.security.ApiKeyAuthenticationFilter;
import com.acme.saas.security.ClerkJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClerkJwtAuthenticationConverter jwtConverter;
    private final ApiKeyAuthenticationFilter apiKeyFilter;

    public SecurityConfig(ClerkJwtAuthenticationConverter jwtConverter,
                          ApiKeyAuthenticationFilter apiKeyFilter) {
        this.jwtConverter = jwtConverter;
        this.apiKeyFilter = apiKeyFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/health", "/health/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**", "/api-docs/**").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Add API key filter before Spring Security OAuth2 filter
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            // Configure OAuth2 JWT validation with custom converter
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            );

        return http.build();
    }
}

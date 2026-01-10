package com.acme.saas.security;

/**
 * Principal representing an authenticated Clerk user.
 *
 * @param userId  The Clerk user ID (from JWT 'sub' claim)
 * @param orgSlug The organization slug (from JWT 'org_slug' claim)
 * @param orgId   The organization ID (from JWT 'org_id' claim)
 * @param orgRole The user's role in the organization (from JWT 'org_role' claim)
 */
public record ClerkPrincipal(
    String userId,
    String orgSlug,
    String orgId,
    String orgRole
) {}
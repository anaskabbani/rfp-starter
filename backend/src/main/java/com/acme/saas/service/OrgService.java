package com.acme.saas.service;

import com.acme.saas.domain.Org;
import com.acme.saas.repository.OrgRepository;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class OrgService {

    private static final Logger log = LoggerFactory.getLogger(OrgService.class);

    private final OrgRepository orgs;
    private final DataSource dataSource;

    // Cache of provisioned schemas to avoid repeated DB queries
    private final ConcurrentMap<String, Boolean> provisionedSchemas = new ConcurrentHashMap<>();

    public OrgService(OrgRepository orgs, DataSource dataSource) {
        this.orgs = orgs;
        this.dataSource = dataSource;
    }

    @Transactional
    public Org createOrg(String slug, String name) {
        String schema = "tenant_" + slug.toLowerCase();
        // 1) create schema if not exists
        try (var c = dataSource.getConnection(); var st = c.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        } catch (Exception e) {
            throw new RuntimeException("Failed creating schema " + schema, e);
        }
        // 2) run tenant migrations for this schema
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();

        // Mark as provisioned
        provisionedSchemas.put(schema, true);

        Org org = new Org(UUID.randomUUID(), slug, name);
        return orgs.save(org);
    }

    /**
     * Ensures that a tenant schema exists for the given organization slug.
     * Used for lazy provisioning when a Clerk organization is accessed for the first time.
     *
     * @param orgSlug The organization slug (e.g., "acme")
     */
    public void ensureSchemaExists(String orgSlug) {
        String schemaName = "tenant_" + orgSlug.toLowerCase();

        // Fast path: already provisioned in this session
        if (provisionedSchemas.containsKey(schemaName)) {
            return;
        }

        // Check if schema exists in database
        if (schemaExists(schemaName)) {
            provisionedSchemas.put(schemaName, true);
            return;
        }

        // Schema doesn't exist - provision it
        log.info("Lazy provisioning schema for new organization: {}", orgSlug);
        provisionSchema(orgSlug);
    }

    private boolean schemaExists(String schemaName) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(
                 "SELECT 1 FROM pg_namespace WHERE nspname = ?")) {
            stmt.setString(1, schemaName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            log.error("Failed to check if schema exists: {}", schemaName, e);
            return false;
        }
    }

    private synchronized void provisionSchema(String orgSlug) {
        String schemaName = "tenant_" + orgSlug.toLowerCase();

        // Double-check after acquiring lock
        if (provisionedSchemas.containsKey(schemaName)) {
            return;
        }

        try {
            // Create schema
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
            }

            // Run tenant migrations
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schemaName)
                    .locations("classpath:db/migration/tenant")
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();

            provisionedSchemas.put(schemaName, true);
            log.info("Successfully provisioned schema: {}", schemaName);
        } catch (Exception e) {
            log.error("Failed to provision schema: {}", schemaName, e);
            throw new RuntimeException("Failed to provision tenant schema: " + schemaName, e);
        }
    }
}

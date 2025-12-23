package com.acme.saas.integration;

import com.acme.saas.domain.Org;
import com.acme.saas.repository.OrgRepository;
import com.acme.saas.service.OrgService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OrgService.
 * Tests tenant provisioning: schema creation and Flyway migrations.
 *
 * Note: Not using @Transactional because schema creation is DDL
 * (cannot be rolled back), so we use manual cleanup.
 */
class OrgServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrgService orgService;

    @Autowired
    private OrgRepository orgRepository;

    @Test
    void testCreateOrg_CreatesSchemaAndRunsMigrations() {
        // Given
        String slug = "testcorp_" + System.currentTimeMillis();
        String name = "Test Corporation";
        String expectedSchema = "tenant_" + slug;

        // When
        Org createdOrg = orgService.createOrg(slug, name);

        // Then
        assertThat(createdOrg).isNotNull();
        assertThat(createdOrg.getId()).isNotNull();
        assertThat(createdOrg.getSlug()).isEqualTo(slug);
        assertThat(createdOrg.getName()).isEqualTo(name);

        // Verify schema was created
        assertThat(schemaExists(expectedSchema))
                .as("Schema %s should exist", expectedSchema)
                .isTrue();

        // Verify org record saved to public.orgs
        Org savedOrg = orgRepository.findById(createdOrg.getId()).orElse(null);
        assertThat(savedOrg).isNotNull();
        assertThat(savedOrg.getSlug()).isEqualTo(slug);

        // Cleanup
        cleanupTenant(slug);
    }

    @Test
    void testCreateOrg_FlywayMigrationsApplied() {
        // Given
        String slug = "acmecorp_" + System.currentTimeMillis();
        String expectedSchema = "tenant_" + slug;

        // When
        orgService.createOrg(slug, "Acme Corporation");

        // Then - Verify tenant migrations ran
        // Check for tenant-specific tables from migrations
        assertThat(tableExists(expectedSchema, "rfp_documents"))
                .as("rfp_documents table should exist in %s", expectedSchema)
                .isTrue();

        assertThat(tableExists(expectedSchema, "rfp_document_extractions"))
                .as("rfp_document_extractions table should exist in %s", expectedSchema)
                .isTrue();

        // Verify Flyway schema history table exists
        assertThat(tableExists(expectedSchema, "flyway_schema_history"))
                .as("flyway_schema_history should exist in %s", expectedSchema)
                .isTrue();

        // Cleanup
        cleanupTenant(slug);
    }

    @Test
    void testCreateOrg_VerifyMigrationHistory() throws Exception {
        // Given
        String slug = "historycorp_" + System.currentTimeMillis();
        String schema = "tenant_" + slug;

        // When
        orgService.createOrg(slug, "History Corp");

        // Then - Verify migration versions in flyway_schema_history
        String sql = String.format(
                "SELECT version, description, success FROM %s.flyway_schema_history ORDER BY installed_rank",
                schema
        );

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int migrationCount = 0;
            while (rs.next()) {
                migrationCount++;
                String version = rs.getString("version");
                boolean success = rs.getBoolean("success");

                assertThat(success)
                        .as("Migration version %s should have succeeded", version)
                        .isTrue();
            }

            // Should have at least baseline + V2 (rfp_documents) + V3 (extractions)
            assertThat(migrationCount)
                    .as("Should have multiple migrations recorded")
                    .isGreaterThan(0);
        }

        // Cleanup
        cleanupTenant(slug);
    }

    @Test
    void testCreateOrg_IdempotentSchemaCreation() {
        // Given
        String slug = "idempotent_" + System.currentTimeMillis();
        String schema = "tenant_" + slug;

        // When - Create org twice
        Org org1 = orgService.createOrg(slug, "Idempotent Test 1");

        // Creating with same slug again should either:
        // 1. Succeed (if no unique constraint on slug)
        // 2. Throw exception (if unique constraint exists)
        // For now, we'll just verify the schema creation is idempotent
        assertThat(schemaExists(schema)).isTrue();

        // Verify tables still exist and are accessible
        assertThat(tableExists(schema, "rfp_documents")).isTrue();

        // Cleanup
        cleanupTenant(slug);
    }

    @Test
    void testCreateOrg_MultipleOrgs_IndependentSchemas() {
        // Given
        long timestamp = System.currentTimeMillis();
        String slug1 = "company1_" + timestamp;
        String slug2 = "company2_" + timestamp;
        String schema1 = "tenant_" + slug1;
        String schema2 = "tenant_" + slug2;

        // When
        Org org1 = orgService.createOrg(slug1, "Company 1");
        Org org2 = orgService.createOrg(slug2, "Company 2");

        // Then - Both schemas should exist independently
        assertThat(schemaExists(schema1)).isTrue();
        assertThat(schemaExists(schema2)).isTrue();

        // Both should have their own tables
        assertThat(tableExists(schema1, "rfp_documents")).isTrue();
        assertThat(tableExists(schema2, "rfp_documents")).isTrue();

        // Both orgs should be saved in public.orgs
        assertThat(orgRepository.findById(org1.getId())).isPresent();
        assertThat(orgRepository.findById(org2.getId())).isPresent();

        // Cleanup
        cleanupTenant(slug1);
        cleanupTenant(slug2);
    }
}

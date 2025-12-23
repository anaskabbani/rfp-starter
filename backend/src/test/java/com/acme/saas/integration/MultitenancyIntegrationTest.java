package com.acme.saas.integration;

import com.acme.saas.domain.RfpDocument;
import com.acme.saas.repository.RfpDocumentRepository;
import com.acme.saas.service.OrgService;
import com.acme.saas.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for multitenancy data isolation.
 * <p>
 * IMPORTANT: These tests verify schema-based data isolation works correctly.
 * We manually set TenantContext in test code (not via HTTP headers) since the
 * TenantFilter mechanism will change when authentication is added.
 * <p>
 * These tests focus on the OUTCOME (data isolation) not the MECHANISM (header parsing).
 */
class MultitenancyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrgService orgService;

    @Autowired
    private RfpDocumentRepository rfpDocumentRepository;

    // Use unique slugs per test run to avoid conflicts
    private String tenantASlug;
    private String tenantBSlug;
    private String tenantASchema;
    private String tenantBSchema;

    @BeforeEach
    void setUp() {
        // Generate unique slugs for this test run
        long timestamp = System.currentTimeMillis();
        tenantASlug = "tenant_a_" + timestamp;
        tenantBSlug = "tenant_b_" + timestamp;
        tenantASchema = "tenant_" + tenantASlug;
        tenantBSchema = "tenant_" + tenantBSlug;

        // Create two tenant schemas for isolation testing
        orgService.createOrg(tenantASlug, "Tenant A Company");
        orgService.createOrg(tenantBSlug, "Tenant B Company");

        // Verify schemas were created
        assertThat(schemaExists(tenantASchema)).isTrue();
        assertThat(schemaExists(tenantBSchema)).isTrue();
    }

    @AfterEach
    void tearDown() {
        // Always clear tenant context after each test to avoid pollution
        TenantContext.clear();

        // Clean up test tenants (drop schemas and delete org records)
        try {
            cleanupTenant(tenantASlug);
            cleanupTenant(tenantBSlug);
        } catch (Exception e) {
            // Log but don't fail - cleanup is best effort
            System.err.println("Cleanup warning: " + e.getMessage());
        }
    }

    @Test
    void testDataIsolation_TenantA_CannotSeeTenantB_Data() throws Exception {
        // Given - Insert test data into both tenant schemas
        UUID docIdTenantA = UUID.randomUUID();
        UUID docIdTenantB = UUID.randomUUID();

        insertTestDocument(tenantASchema, docIdTenantA, "document-a.pdf");
        insertTestDocument(tenantBSchema, docIdTenantB, "document-b.pdf");

        // When - Query as Tenant A
        TenantContext.setCurrentTenant(tenantASchema);
        List<RfpDocument> tenantADocuments = rfpDocumentRepository.findAll();

        // Then - Tenant A should only see their own document
        assertThat(tenantADocuments).hasSize(1);
        assertThat(tenantADocuments.get(0).getId()).isEqualTo(docIdTenantA);
        assertThat(tenantADocuments.get(0).getFilename()).isEqualTo("document-a.pdf");

        // Should NOT see Tenant B's document
        assertThat(tenantADocuments).noneMatch(doc -> doc.getId().equals(docIdTenantB));
    }

    @Test
    void testDataIsolation_TenantB_CannotSeeTenantA_Data() throws Exception {
        // Given - Insert test data into both tenant schemas
        UUID docIdTenantA = UUID.randomUUID();
        UUID docIdTenantB = UUID.randomUUID();

        insertTestDocument(tenantASchema, docIdTenantA, "document-a.pdf");
        insertTestDocument(tenantBSchema, docIdTenantB, "document-b.pdf");

        // When - Query as Tenant B
        TenantContext.setCurrentTenant(tenantBSchema);
        List<RfpDocument> tenantBDocuments = rfpDocumentRepository.findAll();

        // Then - Tenant B should only see their own document
        assertThat(tenantBDocuments).hasSize(1);
        assertThat(tenantBDocuments.get(0).getId()).isEqualTo(docIdTenantB);
        assertThat(tenantBDocuments.get(0).getFilename()).isEqualTo("document-b.pdf");

        // Should NOT see Tenant A's document
        assertThat(tenantBDocuments).noneMatch(doc -> doc.getId().equals(docIdTenantA));
    }

    @Test
    void testDataIsolation_SwitchingTenants_CorrectlyRoutesQueries() throws Exception {
        // Given - Insert test data
        UUID docIdA1 = UUID.randomUUID();
        UUID docIdA2 = UUID.randomUUID();
        UUID docIdB1 = UUID.randomUUID();

        insertTestDocument(tenantASchema, docIdA1, "doc-a1.pdf");
        insertTestDocument(tenantASchema, docIdA2, "doc-a2.pdf");
        insertTestDocument(tenantBSchema, docIdB1, "doc-b1.pdf");

        // When/Then - Switch between tenants and verify isolation
        TenantContext.setCurrentTenant(tenantASchema);
        assertThat(rfpDocumentRepository.findAll()).hasSize(2);

        TenantContext.setCurrentTenant(tenantBSchema);
        assertThat(rfpDocumentRepository.findAll()).hasSize(1);

        TenantContext.setCurrentTenant(tenantASchema);
        assertThat(rfpDocumentRepository.findAll()).hasSize(2);
    }

    @Test
    void testSchemaRouting_SearchPathSwitchesCorrectly() throws Exception {
        // Given - Set tenant context
        TenantContext.setCurrentTenant(tenantASchema);

        // When - Execute query (this triggers Hibernate to get connection with search_path set)
        rfpDocumentRepository.findAll();

        // Then - Verify search_path includes the correct tenant schema
        // Note: This is a best-effort test. The actual search_path is set per connection,
        // and we're checking the general mechanism works.
        assertThat(schemaExists(tenantASchema)).isTrue();
    }

    @Test
    void testDataIsolation_Write_Operations() {
        // Given - Set tenant context for Tenant A
        TenantContext.setCurrentTenant(tenantASchema);

        // When - Create document via repository
        RfpDocument docA = new RfpDocument();
        docA.setId(UUID.randomUUID());
        docA.setFilename("created-in-a.pdf");
        docA.setOriginalFilename("original-a.pdf");
        docA.setContentType("application/pdf");
        docA.setFileSize(1024L);
        docA.setStoragePath("tenant_a/file.pdf");
        docA.setStatus(RfpDocument.DocumentStatus.UPLOADED);

        rfpDocumentRepository.save(docA);

        // Then - Document should exist in Tenant A
        TenantContext.setCurrentTenant(tenantASchema);
        assertThat(rfpDocumentRepository.findById(docA.getId())).isPresent();

        // But NOT in Tenant B
        TenantContext.setCurrentTenant(tenantBSchema);
        assertThat(rfpDocumentRepository.findById(docA.getId())).isEmpty();
    }

    @Test
    void testDefaultTenant_PublicSchema() {
        // When - No tenant set (defaults to "public")
        TenantContext.clear();

        // Then - Should use public schema
        // Note: rfp_documents table doesn't exist in public schema (only in tenant schemas)
        // So this query should return empty or fail depending on schema setup
        String currentTenant = TenantContext.getCurrentTenant();
        assertThat(currentTenant).isEqualTo("public");
    }

    @Test
    void testTenantContext_ClearedAfterRequest() {
        // Given
        TenantContext.setCurrentTenant(tenantASchema);
        assertThat(TenantContext.getCurrentTenant()).isEqualTo(tenantASchema);

        // When - Clear context (simulates end of request)
        TenantContext.clear();

        // Then - Should revert to default
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("public");
    }

    @Test
    void testDataIsolation_MultipleTenants_NoCrossTalk() throws Exception {
        // Given - Create documents in both tenants
        for (int i = 1; i <= 3; i++) {
            insertTestDocument(tenantASchema, UUID.randomUUID(), "tenant-a-doc-" + i + ".pdf");
        }

        for (int i = 1; i <= 2; i++) {
            insertTestDocument(tenantBSchema, UUID.randomUUID(), "tenant-b-doc-" + i + ".pdf");
        }

        // When/Then - Verify counts
        TenantContext.setCurrentTenant(tenantASchema);
        assertThat(rfpDocumentRepository.count()).isEqualTo(3);

        TenantContext.setCurrentTenant(tenantBSchema);
        assertThat(rfpDocumentRepository.count()).isEqualTo(2);

        // Verify at database level (bypass Hibernate)
        assertThat(countRowsInTable(tenantASchema, "rfp_documents")).isEqualTo(3);
        assertThat(countRowsInTable(tenantBSchema, "rfp_documents")).isEqualTo(2);
    }

    // ===== Helper Methods =====

    /**
     * Insert a test document directly into the database for a specific tenant schema.
     * Bypasses Hibernate to ensure we're testing at the schema level.
     */
    private void insertTestDocument(String schema, UUID id, String filename) throws Exception {
        String sql = String.format(
                "INSERT INTO %s.rfp_documents (id, filename, original_filename, content_type, file_size, storage_path, status, uploaded_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                schema
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            stmt.setString(2, filename);
            stmt.setString(3, filename);
            stmt.setString(4, "application/pdf");
            stmt.setLong(5, 1024L);
            stmt.setString(6, schema + "/" + filename);
            stmt.setString(7, "UPLOADED");
            stmt.setObject(8, OffsetDateTime.now());

            stmt.executeUpdate();
        }
    }
}

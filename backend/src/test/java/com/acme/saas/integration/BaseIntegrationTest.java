package com.acme.saas.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Base class for integration tests.
 * Provides Testcontainers PostgreSQL setup and common test utilities.
 *
 * Performance optimizations:
 * - Static container shared across all test classes (starts once)
 * - Each test class gets fresh Spring context (@DirtiesContext) to avoid connection pool issues
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

    // Static container - shared across ALL test classes (starts only once!)
    @Container
    static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true); // Reuse container across test runs if possible
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("aws.s3.bucket", () -> "test-bucket");
    }

    @MockBean
    protected S3Client s3Client; // Mock S3 globally for all integration tests

    @Autowired
    protected DataSource dataSource;

    /**
     * Execute raw SQL statement.
     * Useful for test setup and verification.
     */
    protected void executeSql(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL: " + sql, e);
        }
    }

    /**
     * Execute SQL query and return ResultSet.
     * Note: Caller is responsible for closing the ResultSet, Statement, and Connection.
     */
    protected ResultSet executeQuery(String sql) {
        try {
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute query: " + sql, e);
        }
    }

    /**
     * Check if a schema exists in the database.
     */
    protected boolean schemaExists(String schemaName) {
        String sql = String.format(
                "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = '%s')",
                schemaName
        );

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to check schema existence: " + schemaName, e);
        }
    }

    /**
     * Check if a table exists in a specific schema.
     */
    protected boolean tableExists(String schemaName, String tableName) {
        String sql = String.format(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s')",
                schemaName,
                tableName
        );

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to check table existence: " + schemaName + "." + tableName, e);
        }
    }

    /**
     * Count rows in a table within a specific schema.
     */
    protected int countRowsInTable(String schemaName, String tableName) {
        String sql = String.format("SELECT COUNT(*) FROM %s.%s", schemaName, tableName);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to count rows in: " + schemaName + "." + tableName, e);
        }
    }

    /**
     * Get the current search_path for the connection.
     * Useful for verifying tenant routing.
     */
    protected String getCurrentSearchPath() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW search_path")) {

            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get search_path", e);
        }
    }

    /**
     * Drop a schema if it exists (for cleanup in tests).
     * WARNING: This deletes all data in the schema!
     */
    protected void dropSchemaIfExists(String schemaName) {
        String sql = String.format("DROP SCHEMA IF EXISTS %s CASCADE", schemaName);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to drop schema: " + schemaName, e);
        }
    }

    /**
     * Delete all rows from a table in public schema.
     * Useful for cleanup between tests.
     */
    protected void truncateTable(String tableName) {
        String sql = String.format("TRUNCATE TABLE public.%s CASCADE", tableName);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to truncate table: " + tableName, e);
        }
    }

    /**
     * Delete an org by slug from public.orgs table.
     */
    protected void deleteOrgBySlug(String slug) {
        String sql = "DELETE FROM public.orgs WHERE slug = ?";
        try (Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slug);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete org with slug: " + slug, e);
        }
    }

    /**
     * Clean up test tenant: drop schema and delete org record.
     */
    protected void cleanupTenant(String slug) {
        String schemaName = "tenant_" + slug;
        dropSchemaIfExists(schemaName);
        deleteOrgBySlug(slug);
    }
}

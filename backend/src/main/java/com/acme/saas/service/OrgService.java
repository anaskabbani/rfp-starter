package com.acme.saas.service;

import com.acme.saas.domain.Org;
import com.acme.saas.repository.OrgRepository;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

@Service
public class OrgService {
    private final OrgRepository orgs;
    private final DataSource dataSource;

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

        Org org = new Org(UUID.randomUUID(), slug, name);
        return orgs.save(org);
    }
}

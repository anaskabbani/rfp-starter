package com.acme.saas.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="orgs", schema = "public")
public class Org {
    @Id
    private UUID id;
    private String slug;
    private String name;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Org() {}
    public Org(UUID id, String slug, String name) {
        this.id = id; this.slug = slug; this.name = name;
    }
    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

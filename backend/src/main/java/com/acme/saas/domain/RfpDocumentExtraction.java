package com.acme.saas.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rfp_document_extractions")
public class RfpDocumentExtraction {
    @Id
    private UUID id;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "tables_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String tablesJson;

    @Column(name = "key_values_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String keyValuesJson;

    @Enumerated(EnumType.STRING)
    private ExtractionStatus status;

    private String errorMessage;

    private Integer pageCount;
    private Integer sheetCount;
    private Integer characterCount;
    private Integer tableCount;

    private OffsetDateTime extractedAt;

    public RfpDocumentExtraction() {
        this.id = UUID.randomUUID();
        this.status = ExtractionStatus.PENDING;
    }

    public RfpDocumentExtraction(UUID documentId) {
        this();
        this.documentId = documentId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getTablesJson() { return tablesJson; }
    public void setTablesJson(String tablesJson) { this.tablesJson = tablesJson; }

    public String getKeyValuesJson() { return keyValuesJson; }
    public void setKeyValuesJson(String keyValuesJson) { this.keyValuesJson = keyValuesJson; }

    public ExtractionStatus getStatus() { return status; }
    public void setStatus(ExtractionStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public Integer getSheetCount() { return sheetCount; }
    public void setSheetCount(Integer sheetCount) { this.sheetCount = sheetCount; }

    public Integer getCharacterCount() { return characterCount; }
    public void setCharacterCount(Integer characterCount) { this.characterCount = characterCount; }

    public Integer getTableCount() { return tableCount; }
    public void setTableCount(Integer tableCount) { this.tableCount = tableCount; }

    public OffsetDateTime getExtractedAt() { return extractedAt; }
    public void setExtractedAt(OffsetDateTime extractedAt) { this.extractedAt = extractedAt; }
}

package com.acme.saas.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rfp_documents")
public class RfpDocument {
    @Id
    private UUID id;
    
    private String filename;
    private String originalFilename;
    private String contentType;
    private Long fileSize; // in bytes
    private String storagePath; // path in storage (local filesystem or S3 key)
    
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;
    
    private String errorMessage;
    
    private OffsetDateTime uploadedAt;
    private OffsetDateTime processedAt;
    
    public RfpDocument() {
        this.id = UUID.randomUUID();
        this.uploadedAt = OffsetDateTime.now();
        this.status = DocumentStatus.UPLOADED;
    }
    
    public RfpDocument(String filename, String originalFilename, String contentType, Long fileSize, String storagePath) {
        this();
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
    }
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
    
    public enum DocumentStatus {
        UPLOADED,      // File uploaded, waiting for processing
        PROCESSING,    // Currently being processed
        COMPLETED,     // Successfully processed
        FAILED         // Processing failed
    }
}


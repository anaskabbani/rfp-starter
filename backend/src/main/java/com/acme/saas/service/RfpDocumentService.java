package com.acme.saas.service;

import com.acme.saas.domain.RfpDocument;
import com.acme.saas.repository.RfpDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class RfpDocumentService {
    private final RfpDocumentRepository repository;
    private final FileStorageService storageService;
    
    public RfpDocumentService(RfpDocumentRepository repository, FileStorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }
    
    @Transactional
    public RfpDocument uploadDocument(MultipartFile file, String tenantId) throws IOException {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        // Validate file type (allow PDF, DOCX, DOC, etc.)
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: PDF, DOCX, DOC, TXT");
        }
        
        // Validate file size (max 50MB)
        long maxSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 50MB");
        }
        
        // Store file
        String storagePath = storageService.storeFile(file, tenantId);
        
        // Create document record
        RfpDocument document = new RfpDocument();
        document.setId(UUID.randomUUID());
        document.setFilename(storagePath.substring(storagePath.lastIndexOf('/') + 1));
        document.setOriginalFilename(file.getOriginalFilename());
        document.setContentType(contentType);
        document.setFileSize(file.getSize());
        document.setStoragePath(storagePath);
        document.setStatus(RfpDocument.DocumentStatus.UPLOADED);
        return repository.save(document);
    }
    
    @Transactional(readOnly = true)
    public List<RfpDocument> getAllDocuments() {
        return repository.findAllByOrderByUploadedAtDesc();
    }
    
    @Transactional(readOnly = true)
    public RfpDocument getDocument(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }
    
    @Transactional
    public void deleteDocument(UUID id) throws IOException {
        RfpDocument document = getDocument(id);
        storageService.deleteFile(document.getStoragePath());
        repository.delete(document);
    }
    
    private boolean isAllowedContentType(String contentType) {
        return contentType != null && (
            contentType.equals("application/pdf") ||
            contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || // DOCX
            contentType.equals("application/msword") || // DOC
            contentType.equals("text/plain") // TXT
        );
    }
}


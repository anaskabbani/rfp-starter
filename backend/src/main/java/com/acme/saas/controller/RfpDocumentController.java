package com.acme.saas.controller;

import com.acme.saas.domain.RfpDocument;
import com.acme.saas.service.RfpDocumentService;
import com.acme.saas.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class RfpDocumentController {
    
    private final RfpDocumentService documentService;
    
    public RfpDocumentController(RfpDocumentService documentService) {
        this.documentService = documentService;
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // Get tenant from context (set by TenantFilter)
            String tenantId = extractTenantIdFromContext();
            
            RfpDocument document = documentService.uploadDocument(file, tenantId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", document.getId(),
                "filename", document.getOriginalFilename(),
                "size", document.getFileSize(),
                "status", document.getStatus().toString(),
                "uploadedAt", document.getUploadedAt()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to store file: " + e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<RfpDocument>> listDocuments() {
        List<RfpDocument> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RfpDocument> getDocument(@PathVariable UUID id) {
        try {
            RfpDocument document = documentService.getDocument(id);
            return ResponseEntity.ok(document);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable UUID id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }
    
    private String extractTenantIdFromContext() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.equals(TenantContext.DEFAULT_TENANT)) {
            throw new IllegalStateException("Tenant context not set");
        }
        // Remove "tenant_" prefix if present
        if (tenant.startsWith("tenant_")) {
            return tenant.substring(7);
        }
        return tenant;
    }
}


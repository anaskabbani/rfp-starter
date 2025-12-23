package com.acme.saas.controller;

import com.acme.saas.domain.RfpDocument;
import com.acme.saas.domain.RfpDocumentExtraction;
import com.acme.saas.repository.RfpDocumentExtractionRepository;
import com.acme.saas.service.RfpDocumentService;
import com.acme.saas.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final RfpDocumentExtractionRepository extractionRepository;

    public RfpDocumentController(
            RfpDocumentService documentService,
            RfpDocumentExtractionRepository extractionRepository) {
        this.documentService = documentService;
        this.extractionRepository = extractionRepository;
    }
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @Parameter(description = "Tenant identifier (slug, e.g., 'acme')",
                       required = false,
                       example = "acme",
                       schema = @Schema(type = "string"))
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestParam("file") MultipartFile file) {
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
    public ResponseEntity<List<RfpDocument>> listDocuments(
            @Parameter(description = "Tenant identifier (slug, e.g., 'acme')",
                       required = false,
                       example = "acme",
                       schema = @Schema(type = "string"))
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader) {
        List<RfpDocument> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RfpDocument> getDocument(
            @Parameter(description = "Tenant identifier (slug, e.g., 'acme')",
                       required = false,
                       example = "acme",
                       schema = @Schema(type = "string"))
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @Parameter(description = "Document ID", required = true)
            @PathVariable("id") UUID id) {
        try {
            RfpDocument document = documentService.getDocument(id);
            return ResponseEntity.ok(document);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            @Parameter(description = "Tenant identifier (slug, e.g., 'acme')",
                       required = false,
                       example = "acme",
                       schema = @Schema(type = "string"))
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable("id") UUID id) {
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

    @GetMapping("/{id}/extraction")
    public ResponseEntity<RfpDocumentExtraction> getDocumentExtraction(
            @Parameter(description = "Tenant identifier (slug, e.g., 'acme')",
                       required = false,
                       example = "acme",
                       schema = @Schema(type = "string"))
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable("id") UUID id) {
        return extractionRepository.findByDocumentId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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


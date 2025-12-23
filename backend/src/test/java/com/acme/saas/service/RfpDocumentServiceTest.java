package com.acme.saas.service;

import com.acme.saas.domain.RfpDocument;
import com.acme.saas.repository.RfpDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RfpDocumentServiceTest {

    @Mock
    private RfpDocumentRepository repository;

    @Mock
    private FileStorageService storageService;

    @Mock
    private DocumentExtractionService extractionService;

    @InjectMocks
    private RfpDocumentService documentService;

    private String testTenantId;
    private UUID testDocumentId;

    @BeforeEach
    void setUp() {
        testTenantId = "test-tenant";
        testDocumentId = UUID.randomUUID();
    }

    // ===== File Validation Tests =====

    @Test
    void testUploadDocument_NullFile_ThrowsException() throws Exception {
        // Given
        MultipartFile nullFile = null;

        // When & Then
        assertThatThrownBy(() -> documentService.uploadDocument(nullFile, testTenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File cannot be empty");

        verify(storageService, never()).storeFile(any(), anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void testUploadDocument_EmptyFile_ThrowsException() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                new byte[0] // empty content
        );

        // When & Then
        assertThatThrownBy(() -> documentService.uploadDocument(emptyFile, testTenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File cannot be empty");

        verify(storageService, never()).storeFile(any(), anyString());
    }

    @Test
    void testUploadDocument_InvalidContentType_ThrowsException() throws Exception {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.png",
                "image/png", // not allowed
                "fake image content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> documentService.uploadDocument(invalidFile, testTenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File type not allowed");

        verify(storageService, never()).storeFile(any(), anyString());
    }

    @Test
    void testUploadDocument_ExceedsMaxSize_ThrowsException() throws Exception {
        // Given
        long maxSize = 50 * 1024 * 1024; // 50MB
        long oversizedFileSize = maxSize + 1;

        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                new byte[(int) Math.min(oversizedFileSize, 1024)] // simulate large file
        ) {
            @Override
            public long getSize() {
                return oversizedFileSize;
            }
        };

        // When & Then
        assertThatThrownBy(() -> documentService.uploadDocument(oversizedFile, testTenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum");

        verify(storageService, never()).storeFile(any(), anyString());
    }

    @Test
    void testUploadDocument_AllowedContentTypes_Succeeds() throws Exception {
        // Test all allowed content types
        String[] allowedTypes = {
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // XLSX
                "application/msword", // DOC
                "text/plain" // TXT
        };

        for (String contentType : allowedTypes) {
            // Reset mocks for each iteration
            reset(storageService, repository, extractionService);

            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-file",
                    contentType,
                    "test content".getBytes()
            );

            when(storageService.storeFile(any(), anyString()))
                    .thenReturn("tenant_test/" + UUID.randomUUID() + ".file");
            when(repository.save(any(RfpDocument.class)))
                    .thenAnswer(invocation -> {
                        RfpDocument doc = invocation.getArgument(0);
                        if (doc.getId() == null) {
                            doc.setId(UUID.randomUUID());
                        }
                        return doc;
                    });

            // When
            RfpDocument result = documentService.uploadDocument(file, testTenantId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContentType()).isEqualTo(contentType);
            verify(storageService).storeFile(any(), eq(testTenantId));
        }
    }

    // ===== Upload Flow Tests =====

    @Test
    void testUploadDocument_ValidFile_StoresAndCreatesRecord() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test pdf content".getBytes()
        );

        String expectedStoragePath = "tenant_test/" + UUID.randomUUID() + ".pdf";
        when(storageService.storeFile(file, testTenantId)).thenReturn(expectedStoragePath);

        ArgumentCaptor<RfpDocument> captor = ArgumentCaptor.forClass(RfpDocument.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> {
            RfpDocument doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(testDocumentId);
            }
            return doc;
        });

        // When
        RfpDocument result = documentService.uploadDocument(file, testTenantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginalFilename()).isEqualTo("test.pdf");
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getFileSize()).isEqualTo(file.getSize());
        assertThat(result.getStoragePath()).isEqualTo(expectedStoragePath);

        // Verify extraction ran and status is COMPLETED (since extraction mock succeeds by default)
        assertThat(result.getStatus()).isEqualTo(RfpDocument.DocumentStatus.COMPLETED);

        // Verify repository.save was called twice (once for UPLOADED, once for COMPLETED)
        verify(repository, times(2)).save(any(RfpDocument.class));
    }

    @Test
    void testUploadDocument_ValidFile_TriggersExtraction() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(storageService.storeFile(any(), anyString()))
                .thenReturn("tenant_test/file.pdf");
        when(repository.save(any(RfpDocument.class)))
                .thenAnswer(invocation -> {
                    RfpDocument doc = invocation.getArgument(0);
                    if (doc.getId() == null) {
                        doc.setId(testDocumentId);
                    }
                    return doc;
                });

        // When
        RfpDocument result = documentService.uploadDocument(file, testTenantId);

        // Then
        verify(extractionService).extractDocument(any(RfpDocument.class));
        verify(repository, times(2)).save(any(RfpDocument.class)); // Once before, once after extraction
    }

    @Test
    void testUploadDocument_ExtractionSucceeds_SetsCompletedStatus() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(storageService.storeFile(any(), anyString()))
                .thenReturn("tenant_test/file.pdf");

        ArgumentCaptor<RfpDocument> captor = ArgumentCaptor.forClass(RfpDocument.class);
        when(repository.save(captor.capture()))
                .thenAnswer(invocation -> {
                    RfpDocument doc = invocation.getArgument(0);
                    if (doc.getId() == null) {
                        doc.setId(testDocumentId);
                    }
                    return doc;
                });

        // extractionService succeeds (no exception thrown)

        // When
        RfpDocument result = documentService.uploadDocument(file, testTenantId);

        // Then
        assertThat(result.getStatus()).isEqualTo(RfpDocument.DocumentStatus.COMPLETED);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void testUploadDocument_ExtractionFails_SetsFailedStatus() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(storageService.storeFile(any(), anyString()))
                .thenReturn("tenant_test/file.pdf");
        when(repository.save(any(RfpDocument.class)))
                .thenAnswer(invocation -> {
                    RfpDocument doc = invocation.getArgument(0);
                    if (doc.getId() == null) {
                        doc.setId(testDocumentId);
                    }
                    return doc;
                });

        // Mock extraction failure
        doThrow(new RuntimeException("Extraction failed: corrupted file"))
                .when(extractionService).extractDocument(any(RfpDocument.class));

        // When
        RfpDocument result = documentService.uploadDocument(file, testTenantId);

        // Then
        assertThat(result.getStatus()).isEqualTo(RfpDocument.DocumentStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("Extraction failed");
    }

    // ===== CRUD Tests =====

    @Test
    void testGetAllDocuments_CallsRepositoryFindAll() {
        // Given
        RfpDocument doc1 = new RfpDocument();
        doc1.setId(UUID.randomUUID());
        doc1.setFilename("doc1.pdf");

        RfpDocument doc2 = new RfpDocument();
        doc2.setId(UUID.randomUUID());
        doc2.setFilename("doc2.pdf");

        when(repository.findAllByOrderByUploadedAtDesc())
                .thenReturn(List.of(doc1, doc2));

        // When
        List<RfpDocument> result = documentService.getAllDocuments();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(doc1, doc2);
        verify(repository).findAllByOrderByUploadedAtDesc();
    }

    @Test
    void testGetDocument_ExistingId_ReturnsDocument() {
        // Given
        RfpDocument expectedDocument = new RfpDocument();
        expectedDocument.setId(testDocumentId);
        expectedDocument.setFilename("test.pdf");

        when(repository.findById(testDocumentId))
                .thenReturn(Optional.of(expectedDocument));

        // When
        RfpDocument result = documentService.getDocument(testDocumentId);

        // Then
        assertThat(result).isEqualTo(expectedDocument);
        verify(repository).findById(testDocumentId);
    }

    @Test
    void testGetDocument_NonExistentId_ThrowsException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> documentService.getDocument(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void testDeleteDocument_ExistingId_DeletesFileAndRecord() throws Exception {
        // Given
        RfpDocument document = new RfpDocument();
        document.setId(testDocumentId);
        document.setStoragePath("tenant_test/file.pdf");

        when(repository.findById(testDocumentId))
                .thenReturn(Optional.of(document));

        // When
        documentService.deleteDocument(testDocumentId);

        // Then
        verify(storageService).deleteFile("tenant_test/file.pdf");
        verify(repository).delete(document);
    }

    @Test
    void testDeleteDocument_NonExistentId_ThrowsException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> documentService.deleteDocument(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document not found");

        verify(storageService, never()).deleteFile(anyString());
        verify(repository, never()).delete(any());
    }
}

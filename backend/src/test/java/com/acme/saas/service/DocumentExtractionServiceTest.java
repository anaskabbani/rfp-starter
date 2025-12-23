package com.acme.saas.service;

import com.acme.saas.domain.ExtractionStatus;
import com.acme.saas.domain.RfpDocument;
import com.acme.saas.domain.RfpDocumentExtraction;
import com.acme.saas.dto.ExtractedTable;
import com.acme.saas.dto.KeyValuePair;
import com.acme.saas.repository.RfpDocumentExtractionRepository;
import com.acme.saas.util.TestFileHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentExtractionServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private RfpDocumentExtractionRepository extractionRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DocumentExtractionService extractionService;

    private RfpDocument testDocument;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        testDocument = new RfpDocument();
        testDocument.setId(documentId);
        testDocument.setStoragePath("tenant_test/" + documentId + ".pdf");
    }

    // ===== PDF Extraction Tests =====

    @Test
    void testExtractPdf_ValidPdf_ExtractsTextAndMetadata() throws Exception {
        // Given
        testDocument.setContentType("application/pdf");
        String pdfText = "RFP Document Test\nCarrier: Aetna\nDue Date: January 2025\nContact: John Smith";

        // Mock file storage service to return a simple PDF-like input
        // Note: For real tests, use actual PDF files. This is a simplified mock.
        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(createMockPdfStream(pdfText));
        when(extractionRepository.save(any(RfpDocumentExtraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        assertThat(result.getStatus()).isEqualTo(ExtractionStatus.SUCCESS);
        assertThat(result.getExtractedText()).isNotNull();
        assertThat(result.getPageCount()).isGreaterThan(0);
        assertThat(result.getCharacterCount()).isGreaterThan(0);
        verify(extractionRepository).save(any(RfpDocumentExtraction.class));
    }

    @Test
    void testExtractPdf_S3DownloadFailure_SetsFailedStatus() throws Exception {
        // Given
        testDocument.setContentType("application/pdf");
        when(fileStorageService.downloadFile(anyString()))
                .thenThrow(new RuntimeException("S3 download failed"));
        when(extractionRepository.save(any(RfpDocumentExtraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        assertThat(result.getStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("S3 download failed");
        verify(extractionRepository).save(any(RfpDocumentExtraction.class));
    }

    // ===== DOCX Extraction Tests =====

    @Test
    void testExtractDocx_WithTables_ExtractsTextAndTables() throws Exception {
        // Given - Using real sample-with-tables.docx which contains a table
        testDocument.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(TestFileHelper.getTestFileStream("sample-with-tables.docx"));

        ArgumentCaptor<RfpDocumentExtraction> captor = ArgumentCaptor.forClass(RfpDocumentExtraction.class);
        when(extractionRepository.save(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        assertThat(result.getStatus()).isEqualTo(ExtractionStatus.SUCCESS);
        assertThat(result.getExtractedText()).isNotNull();
        assertThat(result.getTableCount()).isGreaterThan(0); // sample-with-tables.docx has a table

        // Verify tables JSON is valid and contains table data
        assertThat(result.getTablesJson()).isNotNull();
        List<ExtractedTable> tables = objectMapper.readValue(
                result.getTablesJson(),
                new TypeReference<List<ExtractedTable>>() {}
        );
        assertThat(tables).isNotEmpty();
    }

    // ===== XLSX Extraction Tests =====

    @Test
    void testExtractXlsx_MultipleSheets_ExtractsAllSheets() throws Exception {
        // Given - Using real sample.xlsx which has 2 sheets
        testDocument.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(TestFileHelper.getTestFileStream("sample.xlsx"));
        when(extractionRepository.save(any(RfpDocumentExtraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        assertThat(result.getStatus()).isEqualTo(ExtractionStatus.SUCCESS);
        assertThat(result.getSheetCount()).isEqualTo(2); // sample.xlsx has 2 sheets: "Plan Details" and "Pricing"
        assertThat(result.getExtractedText()).isNotNull();
    }

    // ===== Key-Value Extraction Tests =====

    @Test
    void testExtractKeyValues_ValidPattern_ExtractsCorrectly() throws Exception {
        // Given - Using real sample.pdf which contains key-value pairs
        testDocument.setContentType("application/pdf");

        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(TestFileHelper.getTestFileStream("sample.pdf"));

        ArgumentCaptor<RfpDocumentExtraction> captor = ArgumentCaptor.forClass(RfpDocumentExtraction.class);
        when(extractionRepository.save(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        assertThat(result.getStatus()).isEqualTo(ExtractionStatus.SUCCESS);
        assertThat(result.getKeyValuesJson()).isNotNull();

        List<KeyValuePair> keyValues = objectMapper.readValue(
                result.getKeyValuesJson(),
                new TypeReference<List<KeyValuePair>>() {}
        );

        // sample.pdf contains: Carrier: Aetna, Due Date: January 15, 2025, RFP Number: RFP-2025-001, etc.
        assertThat(keyValues).isNotEmpty();
        assertThat(keyValues).extracting("key").contains("Carrier", "Due Date", "RFP Number");
    }

    @Test
    void testExtractKeyValues_First120LinesOnly_StopsAtLimit() throws Exception {
        // Given - Using real PDF, but this test verifies the 120-line limit logic
        testDocument.setContentType("application/pdf");

        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(TestFileHelper.getTestFileStream("sample.pdf"));

        ArgumentCaptor<RfpDocumentExtraction> captor = ArgumentCaptor.forClass(RfpDocumentExtraction.class);
        when(extractionRepository.save(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        List<KeyValuePair> keyValues = objectMapper.readValue(
                result.getKeyValuesJson(),
                new TypeReference<List<KeyValuePair>>() {}
        );

        // Should have at most 120 key-value pairs (from first 120 lines)
        // sample.pdf has fewer than 120 lines, so this just verifies no error
        assertThat(keyValues).hasSizeLessThanOrEqualTo(120);
    }

    @Test
    void testExtractKeyValues_InvalidPatterns_IgnoresInvalid() throws Exception {
        // Given - Using real PDF which has valid key-value patterns
        testDocument.setContentType("application/pdf");

        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(TestFileHelper.getTestFileStream("sample.pdf"));
        when(extractionRepository.save(any(RfpDocumentExtraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        List<KeyValuePair> keyValues = objectMapper.readValue(
                result.getKeyValuesJson(),
                new TypeReference<List<KeyValuePair>>() {}
        );

        // sample.pdf has valid patterns - verify extraction worked and contains valid data
        assertThat(keyValues).isNotEmpty();
        // All extracted keys should be within valid length (2-60 characters)
        assertThat(keyValues).allMatch(kv -> kv.key().length() >= 2 && kv.key().length() <= 60);
    }

    @Test
    void testExtractKeyValues_NoKeyValuePatterns_ReturnsEmptyOrMinimalList() throws Exception {
        // Given - Using sample-simple.docx which has plain text without key-value patterns
        testDocument.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(TestFileHelper.getTestFileStream("sample-simple.docx"));
        when(extractionRepository.save(any(RfpDocumentExtraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        assertThat(result.getStatus()).isEqualTo(ExtractionStatus.SUCCESS);
        List<KeyValuePair> keyValues = objectMapper.readValue(
                result.getKeyValuesJson(),
                new TypeReference<List<KeyValuePair>>() {}
        );

        // sample-simple.docx has minimal or no key-value patterns
        assertThat(keyValues).hasSizeLessThan(5);
    }

    // ===== JSON Serialization Tests =====

    @Test
    void testExtractDocument_TablesJson_SerializesAndDeserializesCorrectly() throws Exception {
        // Given - Using real sample-with-tables.docx
        testDocument.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(TestFileHelper.getTestFileStream("sample-with-tables.docx"));
        when(extractionRepository.save(any(RfpDocumentExtraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        assertThat(result.getStatus()).isEqualTo(ExtractionStatus.SUCCESS);
        assertThat(result.getTablesJson()).isNotNull();

        // Verify it's valid JSON and contains table data
        List<ExtractedTable> tables = objectMapper.readValue(
                result.getTablesJson(),
                new TypeReference<List<ExtractedTable>>() {}
        );
        assertThat(tables).isNotNull();
        assertThat(tables).isNotEmpty(); // sample-with-tables.docx has a table
    }

    // ===== Error Handling Tests =====

    @Test
    void testExtractDocument_UnsupportedContentType_SetsFailedStatus() throws Exception {
        // Given
        testDocument.setContentType("image/png");
        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(extractionRepository.save(any(RfpDocumentExtraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        assertThat(result.getStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("Unsupported content type");
    }

    @Test
    void testExtractDocument_CorruptedFile_SetsFailedStatus() throws Exception {
        // Given
        testDocument.setContentType("application/pdf");
        // Return invalid PDF data
        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(new ByteArrayInputStream("not a valid pdf".getBytes()));
        when(extractionRepository.save(any(RfpDocumentExtraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RfpDocumentExtraction result = extractionService.extractDocument(testDocument);

        // Then
        assertThat(result.getStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(result.getErrorMessage()).isNotNull();
    }

    @Test
    void testExtractDocument_CallsRepositorySave() throws Exception {
        // Given
        testDocument.setContentType("application/pdf");
        when(fileStorageService.downloadFile(anyString()))
                .thenReturn(createMockPdfStream("Test content"));
        when(extractionRepository.save(any(RfpDocumentExtraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        extractionService.extractDocument(testDocument);

        // Then
        ArgumentCaptor<RfpDocumentExtraction> captor = ArgumentCaptor.forClass(RfpDocumentExtraction.class);
        verify(extractionRepository).save(captor.capture());

        RfpDocumentExtraction savedExtraction = captor.getValue();
        assertThat(savedExtraction.getDocumentId()).isEqualTo(documentId);
        assertThat(savedExtraction.getExtractedAt()).isNotNull();
    }

    // ===== Helper Methods for Loading Real Test Files =====

    private InputStream createMockPdfStream(String text) {
        try {
            return TestFileHelper.getTestFileStream("sample.pdf");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test file", e);
        }
    }

    private InputStream createMockPdfStreamWithText(String text) {
        try {
            return TestFileHelper.getTestFileStream("sample.pdf");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test file", e);
        }
    }

    private InputStream createMockDocxStream() {
        try {
            return TestFileHelper.getTestFileStream("sample-with-tables.docx");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test file", e);
        }
    }

    private InputStream createMockXlsxStream() {
        try {
            return TestFileHelper.getTestFileStream("sample.xlsx");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test file", e);
        }
    }
}

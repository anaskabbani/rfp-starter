package com.acme.saas.service;

import com.acme.saas.domain.ExtractionStatus;
import com.acme.saas.domain.RfpDocument;
import com.acme.saas.domain.RfpDocumentExtraction;
import com.acme.saas.dto.ExtractedTable;
import com.acme.saas.dto.ExtractionResult;
import com.acme.saas.dto.KeyValuePair;
import com.acme.saas.repository.RfpDocumentExtractionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentExtractionService {
    private static final Logger log = LoggerFactory.getLogger(DocumentExtractionService.class);
    private static final Pattern KV_PATTERN = Pattern.compile("^([^:]{2,60}):\\s*(.+)$");
    private static final int MAX_KV_LINES = 120;

    private final FileStorageService fileStorageService;
    private final RfpDocumentExtractionRepository extractionRepository;
    private final ObjectMapper objectMapper;

    public DocumentExtractionService(
            FileStorageService fileStorageService,
            RfpDocumentExtractionRepository extractionRepository,
            ObjectMapper objectMapper) {
        this.fileStorageService = fileStorageService;
        this.extractionRepository = extractionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Main extraction method that orchestrates the document processing.
     */
    @Transactional
    public RfpDocumentExtraction extractDocument(RfpDocument document) {
        log.info("Starting extraction for document: {}", document.getId());

        RfpDocumentExtraction extraction = new RfpDocumentExtraction(document.getId());

        try {
            // Download file from S3
            InputStream inputStream = fileStorageService.downloadFile(document.getStoragePath());

            // Route to appropriate extractor based on content type
            ExtractionResult result = switch (document.getContentType()) {
                case "application/pdf" -> extractPdf(inputStream);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> extractDocx(inputStream);
                //clean up the extraction
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> extractXlsx(inputStream);
                default -> throw new IllegalArgumentException("Unsupported content type: " + document.getContentType());
            };

            // Extract key-value pairs from fullText
            List<KeyValuePair> keyValues = extractKeyValues(result.fullText());

            // Populate extraction entity
            extraction.setExtractedText(result.fullText());
            extraction.setTablesJson(objectMapper.writeValueAsString(result.tables()));
            extraction.setKeyValuesJson(objectMapper.writeValueAsString(keyValues));
            extraction.setPageCount(result.pageCount());
            extraction.setSheetCount(result.sheetCount());
            extraction.setCharacterCount(result.characterCount());
            extraction.setTableCount(result.tables().size());
            extraction.setStatus(ExtractionStatus.SUCCESS);
            extraction.setExtractedAt(OffsetDateTime.now());

            log.info("Extraction completed successfully for document: {}", document.getId());

        } catch (Exception e) {
            log.error("Extraction failed for document: {}", document.getId(), e);
            extraction.setStatus(ExtractionStatus.FAILED);
            extraction.setErrorMessage(e.getMessage());
            extraction.setExtractedAt(OffsetDateTime.now());
        }

        return extractionRepository.save(extraction);
    }

    /**
     * Extract text and tables from PDF using Apache PDFBox.
     */
    private ExtractionResult extractPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);

            int pageCount = document.getNumberOfPages();
            int characterCount = fullText.length();

            // Note: PDF table extraction with PDFBox is limited
            // For production, consider Azure Document Intelligence
            List<ExtractedTable> tables = new ArrayList<>();

            return new ExtractionResult(
                    fullText,
                    tables,
                    List.of(), // keyValues extracted separately
                    pageCount,
                    0, // sheetCount not applicable for PDF
                    characterCount
            );
        }
    }

    /**
     * Extract text and tables from DOCX using Apache POI.
     */
    private ExtractionResult extractDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder fullText = new StringBuilder();
            List<ExtractedTable> tables = new ArrayList<>();
            int tableCounter = 1;

            // Extract paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                fullText.append(paragraph.getText()).append("\n");
            }

            // Extract tables
            for (XWPFTable table : document.getTables()) {
                List<List<String>> rows = new ArrayList<>();

                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cells.add(cell.getText().trim());
                    }
                    rows.add(cells);
                }

                if (!rows.isEmpty()) {
                    tables.add(new ExtractedTable("Table " + tableCounter++, rows));
                }
            }

            String text = fullText.toString();
            return new ExtractionResult(
                    text,
                    tables,
                    List.of(), // keyValues extracted separately
                    0, // pageCount not applicable for DOCX
                    0, // sheetCount not applicable for DOCX
                    text.length()
            );
        }
    }

    /**
     * Extract text and tables from XLSX using Apache POI.
     */
    private ExtractionResult extractXlsx(InputStream inputStream) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            StringBuilder fullText = new StringBuilder();
            List<ExtractedTable> tables = new ArrayList<>();
            int sheetCount = workbook.getNumberOfSheets();

            for (int i = 0; i < sheetCount; i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                List<List<String>> rows = new ArrayList<>();

                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        cells.add(cellValue);
                        fullText.append(cellValue).append("\t");
                    }
                    rows.add(cells);
                    fullText.append("\n");
                }

                if (!rows.isEmpty()) {
                    tables.add(new ExtractedTable(sheetName, rows));
                }
            }

            String text = fullText.toString();
            return new ExtractionResult(
                    text,
                    tables,
                    List.of(), // keyValues extracted separately
                    0, // pageCount not applicable for XLSX
                    sheetCount,
                    text.length()
            );
        }
    }

    /**
     * Helper method to get cell value as string from Excel cell.
     */
    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /**
     * Extract key-value pairs from early lines of text.
     * Based on n8n workflow pattern: /^([^:]{2,60}):\s*(.+)$/
     */
    private List<KeyValuePair> extractKeyValues(String fullText) {
        List<KeyValuePair> result = new ArrayList<>();
        if (fullText == null || fullText.isBlank()) {
            return result;
        }

        String[] lines = fullText.split("\n");
        int linesToCheck = Math.min(MAX_KV_LINES, lines.length);

        for (int i = 0; i < linesToCheck; i++) {
            String line = lines[i].trim();
            Matcher matcher = KV_PATTERN.matcher(line);
            if (matcher.matches()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                result.add(new KeyValuePair(key, value));
            }
        }

        return result;
    }
}

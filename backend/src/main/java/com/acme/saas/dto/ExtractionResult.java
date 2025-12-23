package com.acme.saas.dto;

import java.util.List;

public record ExtractionResult(
    String fullText,
    List<ExtractedTable> tables,
    List<KeyValuePair> keyValues,  // From n8n: structured key-value pairs
    int pageCount,
    int sheetCount,
    int characterCount
) {}

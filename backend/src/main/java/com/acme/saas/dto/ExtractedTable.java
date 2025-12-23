package com.acme.saas.dto;

import java.util.List;

public record ExtractedTable(
    String name,  // Sheet name for Excel, "Table 1" for PDF/DOCX
    List<List<String>> rows
) {}

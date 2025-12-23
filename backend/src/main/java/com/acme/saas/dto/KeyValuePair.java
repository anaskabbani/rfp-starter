package com.acme.saas.dto;

public record KeyValuePair(
    String key,    // e.g., "Due Date", "Carrier Name"
    String value   // e.g., "January 2025", "Aetna"
) {}

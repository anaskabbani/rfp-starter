// Document status enum
export type DocumentStatus = "UPLOADED" | "PROCESSING" | "COMPLETED" | "FAILED";

// Extraction status enum
export type ExtractionStatus = "PENDING" | "SUCCESS" | "FAILED";

// RFP Document entity
export interface RfpDocument {
  id: string;
  filename: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  storagePath: string;
  status: DocumentStatus;
  errorMessage?: string;
  uploadedAt: string;
  processedAt?: string;
}

// Extraction result from document parsing
export interface RfpDocumentExtraction {
  id: string;
  documentId: string;
  extractedText?: string;
  tablesJson?: string; // JSON string of ExtractedTable[]
  keyValuesJson?: string; // JSON string of KeyValuePair[]
  status: ExtractionStatus;
  errorMessage?: string;
  pageCount?: number;
  sheetCount?: number;
  characterCount?: number;
  tableCount?: number;
  extractedAt?: string;
}

// Parsed table from extraction
export interface ExtractedTable {
  name: string;
  rows: string[][];
}

// Key-value pair from extraction
export interface KeyValuePair {
  key: string;
  value: string;
}

// Tenant info from whoami endpoint
export interface TenantInfo {
  tenant: string;
  orgId?: string;
  orgSlug?: string;
}

// Upload response
export interface UploadResponse {
  id: string;
  filename: string;
  size: number;
  status: DocumentStatus;
  uploadedAt: string;
}

// Helper to parse tablesJson
export function parseTables(tablesJson?: string): ExtractedTable[] {
  if (!tablesJson) return [];
  try {
    return JSON.parse(tablesJson);
  } catch {
    return [];
  }
}

// Helper to parse keyValuesJson
export function parseKeyValues(keyValuesJson?: string): KeyValuePair[] {
  if (!keyValuesJson) return [];
  try {
    return JSON.parse(keyValuesJson);
  } catch {
    return [];
  }
}

// Format file size for display
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// Format date for display
export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

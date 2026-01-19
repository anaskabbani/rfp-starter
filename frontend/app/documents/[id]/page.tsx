"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useApi } from "@/hooks/useApi";
import type { RfpDocument, RfpDocumentExtraction, DocumentStatus } from "@/types/api";
import { formatFileSize, formatDate } from "@/types/api";
import { ExtractionViewer } from "@/app/components/extraction";
import {
  Button,
  DocumentStatusBadge,
  Card,
  CardHeader,
  CardContent,
  Alert,
  Skeleton,
} from "@/app/components/ui";

export default function DocumentDetailPage() {
  const params = useParams();
  const router = useRouter();
  const api = useApi();
  const documentId = params.id as string;

  const [document, setDocument] = useState<RfpDocument | null>(null);
  const [extraction, setExtraction] = useState<RfpDocumentExtraction | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch document and extraction in parallel
      const [docData, extractionData] = await Promise.all([
        api.getDocument(documentId),
        api.getExtraction(documentId).catch(() => null),
      ]);

      setDocument(docData);
      setExtraction(extractionData);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Failed to load document";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [api, documentId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const getFileTypeLabel = (contentType: string) => {
    if (contentType.includes("pdf")) return "PDF Document";
    if (contentType.includes("word") || contentType.includes("document")) return "Word Document";
    if (contentType.includes("sheet") || contentType.includes("excel")) return "Excel Spreadsheet";
    if (contentType.includes("text/plain")) return "Text File";
    return "Document";
  };

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto">
        <div className="mb-6">
          <Skeleton className="h-8 w-64 mb-2" />
          <Skeleton className="h-4 w-32" />
        </div>
        <Card>
          <CardContent>
            <div className="space-y-4">
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-64 w-full" />
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error || !document) {
    return (
      <div className="max-w-5xl mx-auto">
        <Alert variant="error" className="mb-6">
          {error || "Document not found"}
        </Alert>
        <Button variant="secondary" onClick={() => router.push("/documents")}>
          Back to Documents
        </Button>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <Link
          href="/documents"
          className="inline-flex items-center text-sm text-gray-500 hover:text-gray-700 mb-4"
        >
          <svg
            className="h-4 w-4 mr-1"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M15 19l-7-7 7-7"
            />
          </svg>
          Back to Documents
        </Link>

        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h1 className="text-2xl font-bold text-gray-900 truncate mb-2">
              {document.originalFilename}
            </h1>
            <div className="flex items-center gap-4 text-sm text-gray-500">
              <span>{getFileTypeLabel(document.contentType)}</span>
              <span>{formatFileSize(document.fileSize)}</span>
              <span>Uploaded {formatDate(document.uploadedAt)}</span>
            </div>
          </div>
          <DocumentStatusBadge status={document.status as DocumentStatus} />
        </div>
      </div>

      {/* Document Status Messages */}
      {document.status === "PROCESSING" && (
        <Alert variant="info" className="mb-6">
          <div className="flex items-center gap-2">
            <svg
              className="animate-spin h-4 w-4"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>
            Document is being processed. Please wait...
          </div>
        </Alert>
      )}

      {document.status === "FAILED" && (
        <Alert variant="error" className="mb-6">
          <strong>Extraction failed:</strong>{" "}
          {document.errorMessage || "Unknown error occurred"}
        </Alert>
      )}

      {document.status === "UPLOADED" && (
        <Alert variant="warning" className="mb-6">
          Document has been uploaded but extraction has not started yet.
        </Alert>
      )}

      {/* Extraction Results */}
      {document.status === "COMPLETED" && extraction && (
        <Card>
          <CardHeader>
            <h2 className="text-lg font-semibold text-gray-900">
              Extraction Results
            </h2>
          </CardHeader>
          <CardContent>
            <ExtractionViewer extraction={extraction} />
          </CardContent>
        </Card>
      )}

      {/* No Extraction Available */}
      {document.status === "COMPLETED" && !extraction && (
        <Card>
          <CardContent>
            <div className="text-center py-8 text-gray-500">
              No extraction data available for this document.
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

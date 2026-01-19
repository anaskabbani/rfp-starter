"use client";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { useApi } from "@/hooks/useApi";
import type { RfpDocument, DocumentStatus } from "@/types/api";
import { formatFileSize, formatDate } from "@/types/api";
import FileUpload from "../components/FileUpload";
import {
  Button,
  DocumentStatusBadge,
  Card,
  Alert,
  SkeletonDocumentList,
  EmptyState,
  DocumentIcon,
  ConfirmModal,
} from "../components/ui";

export default function DocumentsPage() {
  const api = useApi();
  const [documents, setDocuments] = useState<RfpDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Delete modal state
  const [deleteModal, setDeleteModal] = useState<{
    isOpen: boolean;
    document: RfpDocument | null;
    loading: boolean;
  }>({ isOpen: false, document: null, loading: false });

  const fetchDocuments = useCallback(async () => {
    try {
      setLoading(true);
      const data = await api.listDocuments();
      setDocuments(data);
      setError(null);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Failed to load documents";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  const handleUploadSuccess = (result: { filename: string }) => {
    setSuccessMessage(`Successfully uploaded: ${result.filename}`);
    fetchDocuments();
  };

  const handleUploadError = (errorMsg: string) => {
    setError(errorMsg);
  };

  const openDeleteModal = (doc: RfpDocument) => {
    setDeleteModal({ isOpen: true, document: doc, loading: false });
  };

  const closeDeleteModal = () => {
    setDeleteModal({ isOpen: false, document: null, loading: false });
  };

  const handleDelete = async () => {
    if (!deleteModal.document) return;

    try {
      setDeleteModal((prev) => ({ ...prev, loading: true }));
      await api.deleteDocument(deleteModal.document.id);
      setSuccessMessage("Document deleted successfully");
      closeDeleteModal();
      fetchDocuments();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Failed to delete document";
      setError(message);
      closeDeleteModal();
    }
  };

  const getFileTypeIcon = (contentType: string) => {
    if (contentType.includes("pdf")) {
      return (
        <div className="h-10 w-10 rounded-lg bg-red-100 flex items-center justify-center">
          <span className="text-red-600 text-xs font-bold">PDF</span>
        </div>
      );
    }
    if (contentType.includes("word") || contentType.includes("document")) {
      return (
        <div className="h-10 w-10 rounded-lg bg-blue-100 flex items-center justify-center">
          <span className="text-blue-600 text-xs font-bold">DOC</span>
        </div>
      );
    }
    if (contentType.includes("sheet") || contentType.includes("excel")) {
      return (
        <div className="h-10 w-10 rounded-lg bg-green-100 flex items-center justify-center">
          <span className="text-green-600 text-xs font-bold">XLS</span>
        </div>
      );
    }
    return (
      <div className="h-10 w-10 rounded-lg bg-gray-100 flex items-center justify-center">
        <span className="text-gray-600 text-xs font-bold">FILE</span>
      </div>
    );
  };

  return (
    <div className="max-w-5xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          RFP Documents
        </h1>
        <p className="text-gray-500">
          Upload and manage your RFP documents for analysis
        </p>
      </div>

      {error && (
        <Alert
          variant="error"
          dismissible
          autoDismiss={5000}
          onDismiss={() => setError(null)}
          className="mb-6"
        >
          {error}
        </Alert>
      )}

      {successMessage && (
        <Alert
          variant="success"
          dismissible
          autoDismiss={5000}
          onDismiss={() => setSuccessMessage(null)}
          className="mb-6"
        >
          {successMessage}
        </Alert>
      )}

      <Card className="mb-8">
        <div className="p-6">
          <FileUpload
            onUploadSuccess={handleUploadSuccess}
            onUploadError={handleUploadError}
          />
        </div>
      </Card>

      <div>
        <h2 className="text-xl font-semibold text-gray-900 mb-4">
          Uploaded Documents
        </h2>

        {loading ? (
          <SkeletonDocumentList count={3} />
        ) : documents.length === 0 ? (
          <Card>
            <EmptyState
              icon={<DocumentIcon className="h-12 w-12" />}
              title="No documents yet"
              description="Upload your first RFP document to get started with automated extraction and analysis."
            />
          </Card>
        ) : (
          <div className="space-y-3">
            {documents.map((doc) => (
              <Card key={doc.id} className="hover:border-gray-300 transition-colors">
                <div className="p-4 flex items-center gap-4">
                  {getFileTypeIcon(doc.contentType)}

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 mb-1">
                      <h3 className="font-medium text-gray-900 truncate">
                        {doc.originalFilename}
                      </h3>
                      <DocumentStatusBadge status={doc.status as DocumentStatus} />
                    </div>
                    <div className="flex items-center gap-4 text-sm text-gray-500">
                      <span>{formatFileSize(doc.fileSize)}</span>
                      <span className="hidden sm:inline">
                        {formatDate(doc.uploadedAt)}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    {doc.status === "COMPLETED" && (
                      <Link href={`/documents/${doc.id}`}>
                        <Button variant="secondary" size="sm">
                          View
                        </Button>
                      </Link>
                    )}
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => openDeleteModal(doc)}
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                    >
                      <svg
                        className="h-4 w-4"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                        />
                      </svg>
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      <ConfirmModal
        isOpen={deleteModal.isOpen}
        onClose={closeDeleteModal}
        onConfirm={handleDelete}
        title="Delete Document"
        message={`Are you sure you want to delete "${deleteModal.document?.originalFilename}"? This action cannot be undone.`}
        confirmText="Delete"
        variant="danger"
        loading={deleteModal.loading}
      />
    </div>
  );
}

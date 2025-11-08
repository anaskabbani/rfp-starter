"use client";

import { useState, useEffect } from "react";
import axios from "axios";
import FileUpload from "../components/FileUpload";

const API = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

interface RfpDocument {
  id: string;
  filename: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  status: string;
  uploadedAt: string;
  processedAt?: string;
}

export default function DocumentsPage() {
  const [documents, setDocuments] = useState<RfpDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const tenantId = "acme"; // In a real app, this would come from auth context

  useEffect(() => {
    fetchDocuments();
  }, []);

  const fetchDocuments = async () => {
    try {
      setLoading(true);
      const response = await axios.get<RfpDocument[]>(`${API}/api/documents`, {
        headers: { "X-Tenant-Id": tenantId },
      });
      setDocuments(response.data);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to load documents");
    } finally {
      setLoading(false);
    }
  };

  const handleUploadSuccess = (result: any) => {
    setSuccessMessage(`Successfully uploaded: ${result.filename}`);
    setTimeout(() => setSuccessMessage(null), 5000);
    fetchDocuments(); // Refresh the list
  };

  const handleUploadError = (errorMsg: string) => {
    setError(errorMsg);
    setTimeout(() => setError(null), 5000);
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Are you sure you want to delete this document?")) {
      return;
    }

    try {
      await axios.delete(`${API}/api/documents/${id}`, {
        headers: { "X-Tenant-Id": tenantId },
      });
      setSuccessMessage("Document deleted successfully");
      setTimeout(() => setSuccessMessage(null), 3000);
      fetchDocuments();
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to delete document");
      setTimeout(() => setError(null), 5000);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + " " + sizes[i];
  };

  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleString();
  };

  const getStatusColor = (status: string): string => {
    switch (status) {
      case "UPLOADED":
        return "#3b82f6"; // blue
      case "PROCESSING":
        return "#f59e0b"; // amber
      case "COMPLETED":
        return "#10b981"; // green
      case "FAILED":
        return "#ef4444"; // red
      default:
        return "#64748b"; // gray
    }
  };

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "24px" }}>
      <h1 style={{ fontSize: "32px", fontWeight: "700", marginBottom: "8px" }}>
        RFP Documents
      </h1>
      <p style={{ color: "#64748b", marginBottom: "32px" }}>
        Upload and manage your RFP documents
      </p>

      {error && (
        <div
          style={{
            backgroundColor: "#fee2e2",
            border: "1px solid #fca5a5",
            color: "#991b1b",
            padding: "12px 16px",
            borderRadius: "6px",
            marginBottom: "24px",
          }}
        >
          {error}
        </div>
      )}

      {successMessage && (
        <div
          style={{
            backgroundColor: "#d1fae5",
            border: "1px solid #6ee7b7",
            color: "#065f46",
            padding: "12px 16px",
            borderRadius: "6px",
            marginBottom: "24px",
          }}
        >
          {successMessage}
        </div>
      )}

      <div style={{ marginBottom: "48px" }}>
        <FileUpload
          tenantId={tenantId}
          onUploadSuccess={handleUploadSuccess}
          onUploadError={handleUploadError}
        />
      </div>

      <div>
        <h2 style={{ fontSize: "24px", fontWeight: "600", marginBottom: "16px" }}>
          Uploaded Documents
        </h2>

        {loading ? (
          <p style={{ color: "#64748b" }}>Loading documents...</p>
        ) : documents.length === 0 ? (
          <p style={{ color: "#64748b" }}>No documents uploaded yet.</p>
        ) : (
          <div
            style={{
              display: "grid",
              gap: "16px",
            }}
          >
            {documents.map((doc) => (
              <div
                key={doc.id}
                style={{
                  border: "1px solid #e2e8f0",
                  borderRadius: "8px",
                  padding: "20px",
                  backgroundColor: "#ffffff",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start" }}>
                  <div style={{ flex: 1 }}>
                    <h3 style={{ fontSize: "18px", fontWeight: "600", marginBottom: "8px" }}>
                      {doc.originalFilename}
                    </h3>
                    <div style={{ display: "flex", gap: "16px", flexWrap: "wrap", fontSize: "14px", color: "#64748b" }}>
                      <span>Size: {formatFileSize(doc.fileSize)}</span>
                      <span>Type: {doc.contentType}</span>
                      <span>Uploaded: {formatDate(doc.uploadedAt)}</span>
                    </div>
                    <div style={{ marginTop: "12px" }}>
                      <span
                        style={{
                          display: "inline-block",
                          padding: "4px 12px",
                          borderRadius: "12px",
                          fontSize: "12px",
                          fontWeight: "500",
                          backgroundColor: getStatusColor(doc.status) + "20",
                          color: getStatusColor(doc.status),
                        }}
                      >
                        {doc.status}
                      </span>
                    </div>
                  </div>
                  <button
                    onClick={() => handleDelete(doc.id)}
                    style={{
                      padding: "8px 16px",
                      backgroundColor: "#fee2e2",
                      color: "#991b1b",
                      border: "none",
                      borderRadius: "6px",
                      cursor: "pointer",
                      fontSize: "14px",
                      fontWeight: "500",
                    }}
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}


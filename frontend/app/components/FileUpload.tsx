"use client";

import { useState, useRef } from "react";
import axios from "axios";

const API = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

interface UploadResult {
  id: string;
  filename: string;
  size: number;
  status: string;
  uploadedAt: string;
}

interface FileUploadProps {
  tenantId?: string;
  onUploadSuccess?: (result: UploadResult) => void;
  onUploadError?: (error: string) => void;
}

export default function FileUpload({ tenantId = "acme", onUploadSuccess, onUploadError }: FileUploadProps) {
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFile = async (file: File) => {
    // Validate file type
    const allowedTypes = [
      "application/pdf",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "application/msword",
      "text/plain"
    ];
    
    if (!allowedTypes.includes(file.type)) {
      onUploadError?.("File type not allowed. Please upload PDF, DOCX, DOC, or TXT files.");
      return;
    }

    // Validate file size (50MB max)
    const maxSize = 50 * 1024 * 1024; // 50MB
    if (file.size > maxSize) {
      onUploadError?.("File size exceeds 50MB limit.");
      return;
    }

    setUploading(true);
    setUploadProgress(0);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await axios.post<UploadResult>(
        `${API}/api/documents/upload`,
        formData,
        {
          headers: {
            "Content-Type": "multipart/form-data",
            "X-Tenant-Id": tenantId,
          },
          onUploadProgress: (progressEvent) => {
            if (progressEvent.total) {
              const percentCompleted = Math.round(
                (progressEvent.loaded * 100) / progressEvent.total
              );
              setUploadProgress(percentCompleted);
            }
          },
        }
      );

      setUploadProgress(100);
      onUploadSuccess?.(response.data);
      
      // Reset after a moment
      setTimeout(() => {
        setUploading(false);
        setUploadProgress(0);
      }, 1000);
    } catch (error: any) {
      const errorMessage = error.response?.data?.error || error.message || "Upload failed";
      onUploadError?.(errorMessage);
      setUploading(false);
      setUploadProgress(0);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    e.preventDefault();
    if (e.target.files && e.target.files[0]) {
      handleFile(e.target.files[0]);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + " " + sizes[i];
  };

  return (
    <div className="file-upload-container">
      <div
        className={`file-upload-area ${dragActive ? "drag-active" : ""} ${uploading ? "uploading" : ""}`}
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        style={{
          border: "2px dashed",
          borderColor: dragActive ? "#3b82f6" : "#cbd5e1",
          borderRadius: "8px",
          padding: "48px 24px",
          textAlign: "center",
          cursor: uploading ? "not-allowed" : "pointer",
          backgroundColor: dragActive ? "#eff6ff" : "#f8fafc",
          transition: "all 0.2s",
        }}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.doc,.docx,.txt"
          onChange={handleChange}
          disabled={uploading}
          style={{ display: "none" }}
        />
        
        {uploading ? (
          <div>
            <div style={{ marginBottom: "16px" }}>
              <div
                style={{
                  width: "100%",
                  height: "8px",
                  backgroundColor: "#e2e8f0",
                  borderRadius: "4px",
                  overflow: "hidden",
                  marginBottom: "8px",
                }}
              >
                <div
                  style={{
                    width: `${uploadProgress}%`,
                    height: "100%",
                    backgroundColor: "#3b82f6",
                    transition: "width 0.3s",
                  }}
                />
              </div>
              <p style={{ color: "#64748b", fontSize: "14px" }}>
                Uploading... {uploadProgress}%
              </p>
            </div>
          </div>
        ) : (
          <div>
            <svg
              width="48"
              height="48"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              style={{ margin: "0 auto 16px", color: "#64748b" }}
            >
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="17 8 12 3 7 8" />
              <line x1="12" y1="3" x2="12" y2="15" />
            </svg>
            <p style={{ fontSize: "16px", fontWeight: "500", marginBottom: "8px", color: "#1e293b" }}>
              Drop your RFP document here, or click to browse
            </p>
            <p style={{ fontSize: "14px", color: "#64748b" }}>
              Supports PDF, DOCX, DOC, TXT (max 50MB)
            </p>
          </div>
        )}
      </div>
    </div>
  );
}


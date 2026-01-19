"use client";

import { useState, useRef } from "react";
import { useApi } from "@/hooks/useApi";
import type { UploadResponse } from "@/types/api";

interface FileUploadProps {
  onUploadSuccess?: (result: UploadResponse) => void;
  onUploadError?: (error: string) => void;
}

const ALLOWED_TYPES = [
  "application/pdf",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "application/msword",
  "text/plain",
];

const MAX_SIZE = 50 * 1024 * 1024; // 50MB

export default function FileUpload({ onUploadSuccess, onUploadError }: FileUploadProps) {
  const api = useApi();
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFile = async (file: File) => {
    // Validate file type
    if (!ALLOWED_TYPES.includes(file.type)) {
      onUploadError?.("File type not allowed. Please upload PDF, DOCX, XLSX, DOC, or TXT files.");
      return;
    }

    // Validate file size
    if (file.size > MAX_SIZE) {
      onUploadError?.("File size exceeds 50MB limit.");
      return;
    }

    setUploading(true);
    setUploadProgress(0);

    try {
      const result = await api.uploadDocument(file, (percent) => {
        setUploadProgress(percent);
      });

      setUploadProgress(100);
      onUploadSuccess?.(result);

      // Reset after animation
      setTimeout(() => {
        setUploading(false);
        setUploadProgress(0);
      }, 1000);
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : "Upload failed";
      onUploadError?.(message);
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

  return (
    <div
      onDragEnter={handleDrag}
      onDragLeave={handleDrag}
      onDragOver={handleDrag}
      onDrop={handleDrop}
      onClick={() => !uploading && fileInputRef.current?.click()}
      className={`
        border-2 border-dashed rounded-xl p-12 text-center transition-all
        ${uploading ? "cursor-not-allowed" : "cursor-pointer"}
        ${
          dragActive
            ? "border-blue-500 bg-blue-50"
            : "border-gray-300 bg-gray-50 hover:border-gray-400 hover:bg-gray-100"
        }
      `}
    >
      <input
        ref={fileInputRef}
        type="file"
        accept=".pdf,.doc,.docx,.xlsx,.txt"
        onChange={handleChange}
        disabled={uploading}
        className="hidden"
      />

      {uploading ? (
        <div className="max-w-xs mx-auto">
          <div className="mb-4">
            <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
              <div
                className="h-full bg-blue-500 transition-all duration-300 ease-out"
                style={{ width: `${uploadProgress}%` }}
              />
            </div>
          </div>
          <div className="flex items-center justify-center gap-2 text-gray-600">
            <svg
              className="animate-spin h-5 w-5 text-blue-500"
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
            <span className="text-sm">Uploading... {uploadProgress}%</span>
          </div>
        </div>
      ) : (
        <div>
          <div className="mb-4 flex justify-center">
            <div className="h-12 w-12 rounded-full bg-blue-100 flex items-center justify-center">
              <svg
                className="h-6 w-6 text-blue-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                />
              </svg>
            </div>
          </div>
          <p className="text-base font-medium text-gray-700 mb-1">
            Drop your RFP document here, or{" "}
            <span className="text-blue-600">browse</span>
          </p>
          <p className="text-sm text-gray-500">
            PDF, DOCX, XLSX, DOC, or TXT (max 50MB)
          </p>
        </div>
      )}
    </div>
  );
}

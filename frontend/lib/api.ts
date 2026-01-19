import axios, { AxiosInstance, AxiosProgressEvent } from "axios";
import type {
  RfpDocument,
  RfpDocumentExtraction,
  TenantInfo,
  UploadResponse,
} from "@/types/api";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

// Create axios instance with base configuration
export function createApiClient(getToken: () => Promise<string | null>): AxiosInstance {
  const client = axios.create({
    baseURL: API_BASE,
  });

  // Add auth token to all requests
  client.interceptors.request.use(async (config) => {
    const token = await getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  return client;
}

// API methods factory
export function createApi(client: AxiosInstance) {
  return {
    // Get current tenant info
    async whoami(): Promise<TenantInfo> {
      const response = await client.get<TenantInfo>("/api/orgs/whoami");
      return response.data;
    },

    // List all documents
    async listDocuments(): Promise<RfpDocument[]> {
      const response = await client.get<RfpDocument[]>("/api/documents");
      return response.data;
    },

    // Get single document
    async getDocument(id: string): Promise<RfpDocument> {
      const response = await client.get<RfpDocument>(`/api/documents/${id}`);
      return response.data;
    },

    // Upload document
    async uploadDocument(
      file: File,
      onProgress?: (percent: number) => void
    ): Promise<UploadResponse> {
      const formData = new FormData();
      formData.append("file", file);

      const response = await client.post<UploadResponse>(
        "/api/documents/upload",
        formData,
        {
          headers: {
            "Content-Type": "multipart/form-data",
          },
          onUploadProgress: (progressEvent: AxiosProgressEvent) => {
            if (onProgress && progressEvent.total) {
              const percent = Math.round(
                (progressEvent.loaded * 100) / progressEvent.total
              );
              onProgress(percent);
            }
          },
        }
      );
      return response.data;
    },

    // Delete document
    async deleteDocument(id: string): Promise<void> {
      await client.delete(`/api/documents/${id}`);
    },

    // Get extraction results
    async getExtraction(id: string): Promise<RfpDocumentExtraction> {
      const response = await client.get<RfpDocumentExtraction>(
        `/api/documents/${id}/extraction`
      );
      return response.data;
    },
  };
}

export type Api = ReturnType<typeof createApi>;

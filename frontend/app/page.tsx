"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useUser, useOrganization } from "@clerk/nextjs";
import { useApi } from "@/hooks/useApi";
import type { RfpDocument, DocumentStatus } from "@/types/api";
import { Card, CardHeader, CardTitle, CardContent, Button, Skeleton } from "./components/ui";

interface DocumentStats {
  total: number;
  uploaded: number;
  processing: number;
  completed: number;
  failed: number;
}

export default function DashboardPage() {
  const { user } = useUser();
  const { organization } = useOrganization();
  const api = useApi();

  const [documents, setDocuments] = useState<RfpDocument[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchDocuments = useCallback(async () => {
    try {
      setLoading(true);
      const data = await api.listDocuments();
      setDocuments(data);
    } catch {
      // Silently fail - user may not have org selected yet
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    if (organization) {
      fetchDocuments();
    } else {
      setLoading(false);
    }
  }, [organization, fetchDocuments]);

  const stats: DocumentStats = {
    total: documents.length,
    uploaded: documents.filter((d) => d.status === "UPLOADED").length,
    processing: documents.filter((d) => d.status === "PROCESSING").length,
    completed: documents.filter((d) => d.status === "COMPLETED").length,
    failed: documents.filter((d) => d.status === "FAILED").length,
  };

  const recentDocuments = documents.slice(0, 5);

  return (
    <div className="max-w-5xl mx-auto">
      {/* Welcome Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Welcome back{user?.firstName ? `, ${user.firstName}` : ""}
        </h1>
        {organization ? (
          <p className="text-gray-500">
            Managing documents for <span className="font-medium text-gray-700">{organization.name}</span>
          </p>
        ) : (
          <p className="text-gray-500">
            Select an organization to get started
          </p>
        )}
      </div>

      {!organization ? (
        <Card>
          <CardContent>
            <div className="text-center py-12">
              <div className="mb-4">
                <svg
                  className="h-12 w-12 text-gray-400 mx-auto"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
                  />
                </svg>
              </div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                No Organization Selected
              </h3>
              <p className="text-gray-500 mb-6 max-w-sm mx-auto">
                Select an organization from the dropdown in the header to view your documents and analytics.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <>
          {/* Stats Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            <StatCard
              label="Total Documents"
              value={loading ? null : stats.total}
              icon={
                <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
              }
            />
            <StatCard
              label="Completed"
              value={loading ? null : stats.completed}
              color="green"
              icon={
                <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M5 13l4 4L19 7" />
                </svg>
              }
            />
            <StatCard
              label="Processing"
              value={loading ? null : stats.processing}
              color="amber"
              icon={
                <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                  />
                </svg>
              }
            />
            <StatCard
              label="Failed"
              value={loading ? null : stats.failed}
              color="red"
              icon={
                <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M6 18L18 6M6 6l12 12" />
                </svg>
              }
            />
          </div>

          {/* Quick Actions & Recent Documents */}
          <div className="grid md:grid-cols-2 gap-6">
            {/* Quick Actions */}
            <Card>
              <CardHeader>
                <CardTitle>Quick Actions</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <Link href="/documents" className="block">
                    <div className="flex items-center gap-4 p-4 rounded-lg border border-gray-200 hover:border-blue-300 hover:bg-blue-50 transition-colors">
                      <div className="h-10 w-10 rounded-lg bg-blue-100 flex items-center justify-center">
                        <svg
                          className="h-5 w-5 text-blue-600"
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
                      <div>
                        <div className="font-medium text-gray-900">Upload Document</div>
                        <div className="text-sm text-gray-500">Add a new RFP document for analysis</div>
                      </div>
                    </div>
                  </Link>

                  <Link href="/documents" className="block">
                    <div className="flex items-center gap-4 p-4 rounded-lg border border-gray-200 hover:border-gray-300 hover:bg-gray-50 transition-colors">
                      <div className="h-10 w-10 rounded-lg bg-gray-100 flex items-center justify-center">
                        <svg
                          className="h-5 w-5 text-gray-600"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M4 6h16M4 10h16M4 14h16M4 18h16"
                          />
                        </svg>
                      </div>
                      <div>
                        <div className="font-medium text-gray-900">View All Documents</div>
                        <div className="text-sm text-gray-500">Browse and manage your documents</div>
                      </div>
                    </div>
                  </Link>
                </div>
              </CardContent>
            </Card>

            {/* Recent Documents */}
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle>Recent Documents</CardTitle>
                {documents.length > 5 && (
                  <Link href="/documents">
                    <Button variant="ghost" size="sm">
                      View all
                    </Button>
                  </Link>
                )}
              </CardHeader>
              <CardContent>
                {loading ? (
                  <div className="space-y-3">
                    {[1, 2, 3].map((i) => (
                      <div key={i} className="flex items-center gap-3">
                        <Skeleton className="h-8 w-8 rounded" />
                        <div className="flex-1">
                          <Skeleton className="h-4 w-3/4 mb-1" />
                          <Skeleton className="h-3 w-1/4" />
                        </div>
                      </div>
                    ))}
                  </div>
                ) : recentDocuments.length === 0 ? (
                  <div className="text-center py-6 text-gray-500">
                    No documents yet. Upload your first document to get started.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {recentDocuments.map((doc) => (
                      <Link
                        key={doc.id}
                        href={doc.status === "COMPLETED" ? `/documents/${doc.id}` : "/documents"}
                        className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 transition-colors"
                      >
                        <FileTypeIcon contentType={doc.contentType} />
                        <div className="flex-1 min-w-0">
                          <div className="text-sm font-medium text-gray-900 truncate">
                            {doc.originalFilename}
                          </div>
                          <div className="text-xs text-gray-500">
                            {getStatusLabel(doc.status as DocumentStatus)}
                          </div>
                        </div>
                        <StatusDot status={doc.status as DocumentStatus} />
                      </Link>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}

function StatCard({
  label,
  value,
  color = "blue",
  icon,
}: {
  label: string;
  value: number | null;
  color?: "blue" | "green" | "amber" | "red";
  icon: React.ReactNode;
}) {
  const colorClasses = {
    blue: "bg-blue-100 text-blue-600",
    green: "bg-green-100 text-green-600",
    amber: "bg-amber-100 text-amber-600",
    red: "bg-red-100 text-red-600",
  };

  return (
    <Card>
      <CardContent className="py-4">
        <div className="flex items-center gap-3">
          <div className={`h-10 w-10 rounded-lg flex items-center justify-center ${colorClasses[color]}`}>
            {icon}
          </div>
          <div>
            {value === null ? (
              <Skeleton className="h-6 w-8 mb-1" />
            ) : (
              <div className="text-2xl font-bold text-gray-900">{value}</div>
            )}
            <div className="text-xs text-gray-500">{label}</div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function FileTypeIcon({ contentType }: { contentType: string }) {
  const getTypeInfo = () => {
    if (contentType.includes("pdf")) return { label: "PDF", bg: "bg-red-100", text: "text-red-600" };
    if (contentType.includes("word") || contentType.includes("document"))
      return { label: "DOC", bg: "bg-blue-100", text: "text-blue-600" };
    if (contentType.includes("sheet") || contentType.includes("excel"))
      return { label: "XLS", bg: "bg-green-100", text: "text-green-600" };
    return { label: "FILE", bg: "bg-gray-100", text: "text-gray-600" };
  };

  const { label, bg, text } = getTypeInfo();

  return (
    <div className={`h-8 w-8 rounded flex items-center justify-center ${bg}`}>
      <span className={`text-xs font-bold ${text}`}>{label}</span>
    </div>
  );
}

function StatusDot({ status }: { status: DocumentStatus }) {
  const colors: Record<DocumentStatus, string> = {
    UPLOADED: "bg-blue-500",
    PROCESSING: "bg-amber-500",
    COMPLETED: "bg-green-500",
    FAILED: "bg-red-500",
  };

  return <div className={`h-2 w-2 rounded-full ${colors[status]}`} />;
}

function getStatusLabel(status: DocumentStatus): string {
  const labels: Record<DocumentStatus, string> = {
    UPLOADED: "Uploaded",
    PROCESSING: "Processing...",
    COMPLETED: "Ready to view",
    FAILED: "Failed",
  };
  return labels[status];
}

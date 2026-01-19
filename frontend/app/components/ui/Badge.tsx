import type { DocumentStatus, ExtractionStatus } from "@/types/api";

type BadgeVariant = "default" | "success" | "warning" | "error" | "info";

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
}

const variantStyles: Record<BadgeVariant, string> = {
  default: "bg-gray-100 text-gray-700",
  success: "bg-green-100 text-green-700",
  warning: "bg-amber-100 text-amber-700",
  error: "bg-red-100 text-red-700",
  info: "bg-blue-100 text-blue-700",
};

export function Badge({ variant = "default", children, className = "" }: BadgeProps) {
  return (
    <span
      className={`
        inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
        ${variantStyles[variant]}
        ${className}
      `}
    >
      {children}
    </span>
  );
}

// Helper to get badge variant from document status
export function getDocumentStatusVariant(status: DocumentStatus): BadgeVariant {
  switch (status) {
    case "COMPLETED":
      return "success";
    case "PROCESSING":
      return "warning";
    case "FAILED":
      return "error";
    case "UPLOADED":
    default:
      return "info";
  }
}

// Helper to get badge variant from extraction status
export function getExtractionStatusVariant(status: ExtractionStatus): BadgeVariant {
  switch (status) {
    case "SUCCESS":
      return "success";
    case "FAILED":
      return "error";
    case "PENDING":
    default:
      return "warning";
  }
}

// Document status badge component
export function DocumentStatusBadge({ status }: { status: DocumentStatus }) {
  return (
    <Badge variant={getDocumentStatusVariant(status)}>
      {status}
    </Badge>
  );
}

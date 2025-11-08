import Link from "next/link";

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body style={{ fontFamily: "ui-sans-serif, system-ui" }}>
        <nav style={{ padding: 16, borderBottom: "1px solid #eee", display: "flex", gap: "24px", alignItems: "center" }}>
          <strong>SaaS Starter</strong>
          <Link href="/" style={{ color: "#3b82f6", textDecoration: "none" }}>Home</Link>
          <Link href="/documents" style={{ color: "#3b82f6", textDecoration: "none" }}>Documents</Link>
        </nav>
        <main style={{ padding: 16 }}>{children}</main>
      </body>
    </html>
  );
}

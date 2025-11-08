"use client";
import { useEffect, useState } from "react";
import axios from "axios";

const API = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

export default function Home() {
  const [tenant, setTenant] = useState<string>("(loading)");

  useEffect(() => {
    axios.get(`${API}/api/orgs/whoami`, { headers: { "X-Tenant-Id": "acme" } })
      .then(r => setTenant(r.data.tenant))
      .catch(() => setTenant("(error)"));
  }, []);

  return (
    <div>
      <h1>Welcome 👋</h1>
      <p>This is the Next.js app talking to Spring Boot.</p>
      <p>Current tenant reported by API: <b>{tenant}</b></p>
      <section style={{ marginTop: 24 }}>
        <h2>Quick Links</h2>
        <p>
          <a href="/documents" style={{ color: "#3b82f6", textDecoration: "none", fontWeight: "500" }}>
            → Upload RFP Documents
          </a>
        </p>
      </section>
      <section style={{ marginTop: 24 }}>
        <h2>Create Org (example)</h2>
        <pre>
{`curl -X POST ${API}/api/orgs \
 -H "Content-Type: application/json" \
 -d '{ "slug":"acme", "name":"Acme Inc" }'`}
        </pre>
      </section>
    </div>
  );
}

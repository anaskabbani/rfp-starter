# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SaaS starter application with Spring Boot 3 (Java 21) backend and Next.js 15 (TypeScript) frontend in a monorepo. Core feature is schema-per-tenant multitenancy with automatic tenant schema provisioning.

## Build & Run Commands

### Initial Setup
```bash
# Start databases (PostgreSQL + Redis)
docker compose up -d

# Build backend
cd backend
mvn clean install -DskipTests

# Install frontend dependencies (first time only)
cd frontend
pnpm install
```

### Development
```bash
# Run backend (Terminal 1)
cd backend
mvn spring-boot:run

# Run frontend (Terminal 2)
cd frontend
pnpm dev
```

### Testing & Building
```bash
# Backend tests
cd backend
mvn test

# Backend build (with tests)
mvn clean install

# Frontend build
cd frontend
pnpm build
pnpm lint
```

### Single Test
```bash
# Run specific test class
cd backend
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName
```

## Architecture

### Multitenancy System

**Core concept**: Each tenant gets its own PostgreSQL schema (`tenant_<slug>`). The system uses a ThreadLocal-based tenant context to route database queries to the correct schema.

**Key components**:
- `TenantContext` (backend/src/main/java/com/acme/saas/tenancy/TenantContext.java): ThreadLocal storage for current tenant. Defaults to "public" schema if no tenant set
- `TenantAuthorizationFilter` (backend/src/main/java/com/acme/saas/security/TenantAuthorizationFilter.java): Extracts tenant from authenticated JWT claims and sets tenant context
- `SchemaPerTenantConnectionProvider`: Hibernate integration that switches schemas based on current tenant
- `CurrentTenantIdentifierResolverImpl`: Resolves tenant identifier from `TenantContext`
- `HibernateConfig` (backend/src/main/java/com/acme/saas/config/HibernateConfig.java): Configures Hibernate with multitenancy beans and CamelCaseToUnderscoresNamingStrategy

**Tenant provisioning flow** (Clerk-driven with lazy provisioning):
1. Admin creates organization in Clerk dashboard with a slug (e.g., "acme")
2. User authenticates via Clerk, JWT includes `org_slug` claim
3. First API request triggers `OrgService.ensureSchemaExists()`:
   - Creates PostgreSQL schema (`CREATE SCHEMA IF NOT EXISTS tenant_<slug>`)
   - Runs Flyway migrations from `db/migration/tenant/` against the new schema
4. Subsequent requests use the existing schema

**Migration locations**:
- `backend/src/main/resources/db/migration/public/`: Shared schema (orgs table, etc.)
- `backend/src/main/resources/db/migration/tenant/`: Per-tenant schema (rfp_documents, rfp_document_extractions, etc.)

**Current tenant migrations**:
- V1: Baseline tenant schema
- V2: RFP documents table
- V3: Document extractions table (text, tables, key-values from PDF/DOCX/XLSX)

**Applying migrations to existing tenants**:
Tenant migrations only run when creating new orgs. For existing tenants, see TODO.md for migration scripts.

### Backend Structure

Standard Spring Boot layered architecture:
- **controller/**: REST endpoints (use `@RestController`, return DTOs/entities)
- **service/**: Business logic (use `@Service`, handle transactions)
- **repository/**: JPA repositories (extend `JpaRepository`)
- **domain/**: JPA entities (use `@Entity`, map to database tables)
- **config/**: Spring configuration classes
- **security/**: Authentication and authorization (Clerk JWT, API key filters)
- **tenancy/**: Multitenancy infrastructure (TenantContext, Hibernate providers)

**Important**: All JPA queries automatically route to the current tenant's schema via Hibernate multitenancy configuration.

### Document Extraction System

**Purpose**: Automatically extract text, tables, and key-value pairs from uploaded RFP documents (PDF, DOCX, XLSX) to enable AI-powered analysis.

**Supported formats**:
- PDF (via Apache PDFBox 3.0.1)
- DOCX (via Apache POI 5.2.5)
- XLSX (via Apache POI 5.2.5)

**Extraction flow**:
1. User uploads document → stored in S3
2. `RfpDocumentService.uploadDocument()` triggers extraction synchronously
3. `DocumentExtractionService` downloads file from S3 and routes to appropriate extractor
4. Extracted data saved to `rfp_document_extractions` table (per-tenant schema)
5. Document status updated to COMPLETED or FAILED

**Extracted data structure**:
- `extractedText` (TEXT): Full document text content
- `tablesJson` (JSONB): Array of tables with structure `[{name: "Sheet1", rows: [[cell, cell], ...]}]`
- `keyValuesJson` (JSONB): Key-value pairs from document headers `[{key: "Due Date", value: "Jan 2025"}]`
- Metadata: pageCount, sheetCount, characterCount, tableCount

**Key components**:
- `DocumentExtractionService` (backend/src/main/java/com/acme/saas/service/DocumentExtractionService.java): Main extraction orchestrator
- `RfpDocumentExtraction` (domain entity): Stores extraction results with JSONB columns
- DTOs: `ExtractionResult`, `ExtractedTable`, `KeyValuePair`

**API endpoints**:
- `POST /api/documents/upload` - Uploads document and triggers extraction
- `GET /api/documents/{id}/extraction` - Retrieves extraction results

**JSONB type mapping**:
Entity fields using JSONB must include `@JdbcTypeCode(SqlTypes.JSON)` annotation for Hibernate 6:
```java
@Column(name = "tables_json", columnDefinition = "JSONB")
@JdbcTypeCode(SqlTypes.JSON)
private String tablesJson;
```

**Key-value extraction**:
Parses first 120 lines of text using regex pattern `/^([^:]{2,60}):\s*(.+)$/` to extract structured metadata like "Carrier: Aetna", "Due Date: January 2025" (based on n8n workflow analysis).

**Limitations**:
- PDF table extraction is basic (PDFBox has limited table detection)
- Extraction runs synchronously (may timeout on large files)
- No OCR support for scanned documents
- Future: Consider Azure Document Intelligence for better PDF table extraction

### Frontend Structure

Next.js 15 using App Router:
- `frontend/app/`: App Router pages and layouts
- `frontend/app/components/`: Shared React components
- `frontend/app/documents/`: RFP document upload feature

API calls use axios with base URL `http://localhost:8080` (dev) or `NEXT_PUBLIC_API_BASE` env var.

### Authentication System

**Provider**: Clerk (clerk.com) for user authentication and organization management.

**Authentication methods**:
1. **JWT (primary)**: Clerk-issued JWTs validated via JWKS endpoint. Tenant derived from `org_slug` claim.
2. **API Key (service-to-service)**: `X-API-Key` header for internal services. Tenant from `X-Tenant-Id` header.

**Key components**:
- `SecurityConfig` (backend/src/main/java/com/acme/saas/config/SecurityConfig.java): Configures OAuth2 Resource Server with JWT validation
- `ClerkJwtAuthenticationConverter`: Extracts `org_slug`, `org_id`, `org_role` from JWT claims
- `ClerkPrincipal`: Record holding authenticated user info (userId, orgSlug, orgId, orgRole)
- `ClerkAuthenticationToken`: Spring Security token wrapping ClerkPrincipal
- `ApiKeyAuthenticationFilter`: Processes `X-API-Key` header for service auth
- `TenantAuthorizationFilter`: Sets TenantContext from authenticated principal

**Public endpoints** (no auth required):
- `/health`, `/health/**`
- `/swagger-ui/**`, `/swagger-ui.html`
- `/v3/api-docs/**`, `/api-docs/**`

**Authentication flow**:
1. Frontend obtains JWT from Clerk after user login
2. Frontend sends `Authorization: Bearer <jwt>` header with requests
3. Backend validates JWT signature via Clerk's JWKS endpoint
4. `ClerkJwtAuthenticationConverter` extracts claims and creates `ClerkPrincipal`
5. `TenantAuthorizationFilter` sets tenant context from `org_slug`
6. If schema doesn't exist, lazy provisioning creates it

**Testing authentication**:
```bash
# With Clerk JWT
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/api/documents

# With API key (for service-to-service)
curl -H "X-API-Key: <key>" -H "X-Tenant-Id: acme" http://localhost:8080/api/documents
```

## Configuration

### Backend (`backend/src/main/resources/application.properties`)

Key settings:
- Database: `DB_URL`, `DB_USER`, `DB_PASS` env vars (defaults to localhost:5432)
- AWS S3: `S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (credentials optional if using IAM roles)
- Clerk Auth: `CLERK_ISSUER_URI`, `CLERK_JWKS_URI` for JWT validation
- API Keys: `API_KEYS` (comma-separated list for service-to-service auth)
- Multitenancy: Configured via `HibernateConfig` Java class (not properties)
- Flyway: `spring.flyway.locations=classpath:db/migration/public` for public schema
- Naming strategy: CamelCaseToUnderscoresNamingStrategy for DB column mapping

### Frontend (`frontend/package.json`)

Scripts:
- `pnpm dev`: Development server (localhost:3000)
- `pnpm build`: Production build
- `pnpm start`: Production server

Environment variable: `NEXT_PUBLIC_API_BASE` for API URL override.

## Database Access

Development database credentials (from `docker-compose.yml`):
```bash
docker compose exec db psql -U app -d app

# Or via connection string
psql postgresql://app:app@localhost:5432/app
```

## AWS S3 Setup

File storage uses AWS S3. Before running the application:

1. **Create S3 bucket**:
   ```bash
   aws s3 mb s3://your-bucket-name --region us-east-1
   ```

2. **Configure credentials** (choose one):
   ```bash
   # Option 1: Environment variables (recommended for dev)
   export AWS_ACCESS_KEY_ID=your_access_key
   export AWS_SECRET_ACCESS_KEY=your_secret_key
   export AWS_REGION=us-east-1
   export S3_BUCKET=your-bucket-name

   # Option 2: Add to application.properties
   aws.access.key.id=your_access_key
   aws.secret.access.key=your_secret_key

   # Option 3: Use IAM roles (recommended for production)
   # No explicit credentials needed - automatically uses EC2/ECS task role
   ```

3. **Required S3 permissions**:
   - `s3:PutObject` - for uploads
   - `s3:DeleteObject` - for deletes
   - `s3:HeadObject` - for existence checks

## Key Implementation Patterns

### Adding a New Tenant-Scoped Entity

1. Create JPA entity in `backend/src/main/java/com/acme/saas/domain/`
2. Create repository interface extending `JpaRepository`
3. Add Flyway migration to `backend/src/main/resources/db/migration/tenant/V{N}__{description}.sql`
4. Hibernate multitenancy automatically routes queries to current tenant schema
5. No need to manually specify schema in queries

### Creating New API Endpoints

1. Add controller method with `@GetMapping`/`@PostMapping` in `controller/` package
2. Use `@RequestBody` for JSON payloads, `@PathVariable` for URL params
3. OpenAPI (Swagger) docs auto-generated at `http://localhost:8080/swagger-ui.html`
4. All endpoints require authentication (JWT or API key) unless added to public endpoints in `SecurityConfig`
5. Tenant context automatically set from JWT `org_slug` claim via `TenantAuthorizationFilter`
6. Return entities/DTOs directly - Jackson handles JSON serialization

### File Uploads

- Handled by `FileStorageService` (stores in AWS S3 with keys: `tenant_<id>/<uuid>.<ext>`)
- S3 configuration in `S3Config` with flexible credentials (properties, env vars, or IAM roles)
- Multipart config in `application.properties`: max 50MB per file
- `RfpDocumentController.uploadDocument` endpoint accepts multipart files
- Frontend drag-and-drop component in `frontend/app/documents/page.tsx`
- File operations: `storeFile()`, `deleteFile()`, `fileExists()` - all interact with S3

## CI/CD

GitHub Actions workflow (`.github/workflows/ci.yml`):
- Backend: `mvn clean install -DskipTests`
- Frontend: `pnpm i && pnpm build`
- Runs on push to `main` and on PRs

## Dependencies

**Backend** (managed via Maven in `backend/pom.xml`):
- Spring Boot 3.3.4 with Web, Security, Data JPA, Validation
- Spring Boot OAuth2 Resource Server for JWT validation
- Flyway 10.10.0 for migrations
- PostgreSQL driver
- AWS SDK v2 S3 (2.20.26) for file storage
- Apache PDFBox 3.0.1 for PDF text extraction
- Apache POI 5.2.5 (poi-ooxml) for DOCX/XLSX extraction
- SpringDoc OpenAPI 2.6.0 for API docs
- Testcontainers for integration tests

**Frontend** (managed via pnpm in `frontend/package.json`):
- Next.js 15.0.3
- React 18.3.1
- TypeScript 5.6.2
- Axios 1.7.7 for HTTP requests
- Zod 3.23.8 for validation

## Common Gotchas

- **Authentication required**: All API endpoints (except `/health`, `/swagger-ui/**`) require authentication. Use `Authorization: Bearer <jwt>` header with Clerk JWT.
- **Tenant from JWT**: Tenant is automatically derived from the JWT `org_slug` claim. No need to set `X-Tenant-Id` header for authenticated requests. (API key auth still uses `X-Tenant-Id` header.)
- **Clerk org_slug required**: JWT must contain `org_slug` claim. If user isn't part of a Clerk organization, requests will fail with 403.
- **Lazy provisioning**: Tenant schemas are created automatically on first authenticated request. No need to manually create orgs via API.
- **Migrations**: Public schema migrations run automatically on app startup. Tenant schema migrations run on first request to a new tenant (lazy provisioning). For existing tenants, see TODO.md for manual migration scripts.
- **Column naming**: Java camelCase fields (e.g., `contentType`) automatically map to snake_case DB columns (e.g., `content_type`) via `CamelCaseToUnderscoresNamingStrategy`
- **JSONB type mapping**: Entity fields storing JSONB must use `@JdbcTypeCode(SqlTypes.JSON)` annotation in addition to `columnDefinition = "JSONB"`. The `columnDefinition` only affects DDL generation; `@JdbcTypeCode` tells Hibernate how to map the type during queries.
- **S3 configuration**: Requires S3 bucket to exist and proper AWS credentials (env vars, properties, or IAM roles). In production, use IAM roles for security.
- **File storage**: Files are stored in S3 with keys like `tenant_<id>/<uuid>.<ext>`. Ensure S3 bucket has proper permissions (PutObject, DeleteObject, HeadObject, GetObject).
- **Document extraction**: Runs synchronously during upload. Large files (>10MB) may cause timeouts. Future: Move to async processing.
- **Swagger path variables**: Must explicitly specify parameter name in `@PathVariable("id")` for Swagger to display the input field correctly.
- **Maven PATH**: On macOS with Homebrew, may need `export PATH="/opt/homebrew/bin:$PATH"`
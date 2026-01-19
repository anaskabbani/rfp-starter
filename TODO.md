# TODO - SaaS Starter JVM

> **Note**: See ARCHITECTURE_REVIEW.md for detailed security and scalability concerns.

## 🚨 Critical Security & Production Readiness

**These items MUST be completed before production deployment** (from Architecture Review)

### ✅ COMPLETE - Authentication & Authorization
**Priority: CRITICAL | Blocks Production: YES | Status: Complete (Jan 10, 2026)**

**Backend Implementation Complete** ✅
- [x] Implement OAuth2/JWT authentication (Spring Security OAuth2 Resource Server)
- [x] Clerk as authentication provider (JWT validation via JWKS)
- [x] Tenant derived from JWT `org_slug` claim (not header)
- [x] API key authentication for service-to-service calls
- [x] Lazy schema provisioning for new Clerk organizations
- [x] Updated Swagger/OpenAPI with Bearer auth scheme

**Remaining Steps**:

1. **Set up Clerk account**:
   - [x] Create account at [clerk.com](https://clerk.com)
   - [x] Create an organization with a slug (e.g., "acme")
   - [x] Note your instance URL (e.g., `https://your-instance.clerk.accounts.dev`)

2. **Configure environment variables**:
   ```bash
   export CLERK_ISSUER_URI=https://your-instance.clerk.accounts.dev
   export CLERK_JWKS_URI=https://your-instance.clerk.accounts.dev/.well-known/jwks.json
   export API_KEYS=your-api-key-here  # For service-to-service calls (optional)
   ```

3. **Test backend auth**:
   - [x] Get a test JWT from Clerk dashboard
   - [x] Test endpoints with `Authorization: Bearer <jwt>`
   - [x] Verify tenant schema is lazily created
   - [x] Verify unauthenticated requests return 401

4. **Implement frontend Clerk integration**: ✅ **COMPLETED (Jan 10, 2026)**
   - [x] Install `@clerk/nextjs` package
   - [x] Add ClerkProvider to layout
   - [x] Add sign-in/sign-out components
   - [x] Add JWT to API requests via axios interceptor
   - [x] Add middleware for protected routes
   - [x] Add OrganizationSwitcher for multi-org support
   - [x] Build complete UI with Tailwind CSS

**Files Created**:
- `security/ClerkPrincipal.java` - User principal record
- `security/ClerkAuthenticationToken.java` - Auth token
- `security/ClerkJwtAuthenticationConverter.java` - JWT → principal converter
- `security/TenantAuthorizationFilter.java` - Sets tenant context from auth
- `security/ApiKeyAuthenticationToken.java` - API key token
- `security/ApiKeyAuthenticationFilter.java` - API key filter

**Files Modified**:
- `config/SecurityConfig.java` - OAuth2 resource server config
- `config/OpenApiConfig.java` - Bearer auth in Swagger
- `service/OrgService.java` - Lazy schema provisioning
- `application.properties` - Clerk JWT + API key config

**Files Removed**:
- `tenancy/TenantFilter.java` - Replaced by TenantAuthorizationFilter

---

### 🟡 HIGH - Integration Tests (HTTP Layer & Authentication)
**Priority: HIGH | Blocks Production: YES | Effort: 10 days (2 weeks)**

**Status**: Data-layer tests exist ✅, HTTP-layer tests needed ❌

**What Already Exists**:
- ✅ `MultitenancyIntegrationTest.java` - Data-layer tenant isolation
- ✅ `OrgServiceIntegrationTest.java` - Org creation + schema provisioning + Flyway migrations
- ✅ `DocumentExtractionIntegrationTest.java` - PDF/DOCX/XLSX extraction with S3 mocking
- ✅ `BaseIntegrationTest.java` - Testcontainers infrastructure, DB helper methods
- ✅ Unit tests for service layer with mocks
- ✅ Test file utilities and sample documents

**Critical Gaps** (blocks production):
1. **Authentication/Authorization** - Zero tests for Clerk JWT or API key flows
2. **HTTP-layer tenant isolation** - No end-to-end tests via HTTP (all current tests bypass HTTP)
3. **Security filter chain** - TenantAuthorizationFilter, ApiKeyAuthenticationFilter untested
4. **API contracts** - No validation of HTTP status codes, error responses, response schemas
5. **Lazy provisioning via auth** - No tests that first authenticated request creates schema

**Implementation Plan**: See **`/Users/anaskabbani/.claude/plans/zippy-honking-cook.md`** for detailed 10-day implementation plan covering:
- Phase 1 (Days 1-3): Authentication integration tests (JWT flows, API key auth, security filters)
- Phase 2 (Days 4-5): End-to-end tenant isolation via HTTP
- Phase 3 (Days 6-7): API contract tests (all endpoints, status codes, response schemas)
- Phase 4 (Day 8): OpenAPI contract validation
- Phase 5 (Days 9-10): Edge cases and error handling

**Tools**: Testcontainers (PostgreSQL), JUnit 5, Spring Boot Test, TestRestTemplate, JWT mocking

---

### 🟡 HIGH - Observability & Monitoring
**Priority: HIGH | Blocks Production: NO (but risky) | Effort: 1 week**

**Problem**: Can't diagnose production issues without metrics, tracing, and structured logging.

**Required Implementation**:
- [ ] Configure OpenTelemetry exporter (Jaeger, Honeycomb, or Datadog)
- [ ] Add custom metrics for tenant operations (per-tenant request rate, errors)
- [ ] Implement structured logging with tenant context in MDC
- [ ] Add readiness/liveness health checks for Kubernetes
- [ ] Add metrics endpoint for Prometheus scraping

**Files to Modify**:
- `application.properties` - OpenTelemetry configuration
- New: `config/ObservabilityConfig.java`
- New: `health/ReadinessProbe.java`
- New: `health/LivenessProbe.java`

---

### 🟡 MEDIUM - ThreadLocal Tenant Context Limitations
**Priority: MEDIUM | Blocks Production: NO (unless async needed) | Effort: 1 week**

**Problem**: Current ThreadLocal-based TenantContext is incompatible with:
- `@Async` methods (tenant context lost on different threads)
- `CompletableFuture` and async operations
- Spring `@Scheduled` tasks
- Java 21 virtual threads (can be unmounted/remounted)

**Impact**: Limits concurrency model and future scalability.

**Solutions**:
- [ ] **Short term**: Document the limitation, avoid async operations
- [ ] **Medium term**: Consider Spring's `RequestContextHolder` or Java 21's `ScopedValue`
- [ ] **Long term**: If going reactive, migrate to Project Reactor's `Context` API

**Decision Needed**:
- Are you planning to use Java 21 virtual threads?
- Any async operations needed (background jobs, document processing)?

---

### 🟢 LOW - Redis: Remove or Implement
**Priority: LOW | Blocks Production: NO | Effort: 1 hour**

**Problem**: Redis container runs but nothing uses it. Creates confusion and wastes resources.

**Options**:
- [ ] **Option A**: Remove from docker-compose.yml if not planned
- [ ] **Option B**: Implement caching for tenant metadata (avoid DB lookup per request)
- [ ] **Option C**: Use for session storage, rate limiting, or distributed locks

**Decision Needed**: What was Redis intended for?

---

## Backend API Gaps

> **Identified during frontend implementation (Jan 10, 2026)**
> These gaps don't block MVP but should be addressed for production readiness.

### 🟡 HIGH - Document List Pagination
**Priority: HIGH | Blocks Production: NO (until scale) | Effort: 2-4 hours**

**Problem**: `GET /api/documents` returns all documents. Performance degrades with many documents.

**Implementation**:
- [ ] Add `page` and `size` query parameters
- [ ] Return paginated response with total count
- [ ] Use Spring Data `Pageable` in repository
- [ ] Update frontend to handle pagination

**Files to Modify**:
- `RfpDocumentController.java` - Add pagination params
- `RfpDocumentRepository.java` - Use `Pageable`
- Frontend `useApi.ts` - Handle paginated response

---

### 🟡 MEDIUM - Document Search & Filtering
**Priority: MEDIUM | Blocks Production: NO | Effort: 4-6 hours**

**Problem**: No way to search or filter documents by name, status, or date.

**Implementation**:
- [ ] Add `search` query param (filename search)
- [ ] Add `status` query param (filter by UPLOADED/PROCESSING/COMPLETED/FAILED)
- [ ] Add `dateFrom`/`dateTo` query params
- [ ] Use Spring Data Specifications or Criteria API

**Files to Modify**:
- `RfpDocumentController.java` - Add filter params
- `RfpDocumentRepository.java` - Add query methods
- New: `RfpDocumentSpecification.java` for dynamic queries

---

### 🟡 MEDIUM - File Download Endpoint
**Priority: MEDIUM | Blocks Production: NO | Effort: 2-3 hours**

**Problem**: No endpoint to retrieve original uploaded files from S3.

**Implementation**:
- [ ] Add `GET /api/documents/{id}/download` endpoint
- [ ] Return file with proper Content-Type and Content-Disposition headers
- [ ] Consider presigned S3 URL vs streaming through backend
- [ ] Add rate limiting to prevent abuse

**Files to Modify**:
- `RfpDocumentController.java` - Add download endpoint
- `FileStorageService.java` - Add getFile or getPresignedUrl method

---

### 🟡 MEDIUM - Extraction Retry Endpoint
**Priority: MEDIUM | Blocks Production: NO | Effort: 2-3 hours**

**Problem**: Failed extractions have no retry mechanism. Documents stuck in FAILED state.

**Implementation**:
- [ ] Add `POST /api/documents/{id}/retry-extraction` endpoint
- [ ] Reset extraction status to PENDING
- [ ] Re-run extraction synchronously or queue for async processing
- [ ] Add retry count tracking to prevent infinite retries

**Files to Modify**:
- `RfpDocumentController.java` - Add retry endpoint
- `RfpDocumentService.java` - Add retry logic
- `RfpDocumentExtraction.java` - Add retryCount field (optional)

---

### 🟢 LOW - Organization List/Detail Endpoints
**Priority: LOW | Blocks Production: NO | Effort: 2-3 hours**

**Problem**: Only `createOrg` and `whoami` exist. No way to list or view org details.

**Implementation**:
- [ ] Add `GET /api/orgs` - List user's organizations (from Clerk)
- [ ] Add `GET /api/orgs/{slug}` - Get org details
- [ ] Consider whether this should call Clerk API or just return cached data

**Note**: Clerk handles org management. These endpoints may be unnecessary if using Clerk's organization UI.

---

### 🟢 LOW - Bulk Document Operations
**Priority: LOW | Blocks Production: NO | Effort: 4-6 hours**

**Problem**: Can only delete one document at a time. No bulk operations.

**Implementation**:
- [ ] Add `POST /api/documents/bulk-delete` with array of IDs
- [ ] Add transaction handling for atomic operations
- [ ] Add maximum batch size limit

**Files to Modify**:
- `RfpDocumentController.java` - Add bulk endpoints
- `RfpDocumentService.java` - Add bulk operation methods

---

## Immediate Tasks

### 1. Apply V3 Migration to Existing Tenant Schemas
**Priority: HIGH**

The new `rfp_document_extractions` table migration (V3) only applies to newly created tenants. Existing tenant schemas need the migration applied manually.

**Options:**

**Option A: SQL Script (Recommended for one-time migration)**
```sql
DO $$
DECLARE
    schema_name text;
BEGIN
    FOR schema_name IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname LIKE 'tenant_%'
    LOOP
        EXECUTE format('SET search_path TO %I', schema_name);

        EXECUTE '
            CREATE TABLE IF NOT EXISTS rfp_document_extractions (
              id UUID PRIMARY KEY,
              document_id UUID NOT NULL REFERENCES rfp_documents(id) ON DELETE CASCADE,
              extracted_text TEXT,
              tables_json JSONB,
              key_values_json JSONB,
              status TEXT NOT NULL DEFAULT ''PENDING'',
              error_message TEXT,
              page_count INT,
              sheet_count INT,
              character_count INT,
              table_count INT,
              extracted_at TIMESTAMPTZ,
              CONSTRAINT fk_document FOREIGN KEY (document_id) REFERENCES rfp_documents(id)
            );

            CREATE INDEX IF NOT EXISTS idx_extractions_document_id ON rfp_document_extractions(document_id);
            CREATE INDEX IF NOT EXISTS idx_extractions_status ON rfp_document_extractions(status);
        ';

        RAISE NOTICE 'Migrated schema: %', schema_name;
    END LOOP;
END $$;
```

Run with: `psql postgresql://app:app@localhost:5432/app -f migrate_tenants.sql`

**Option B: Add Automatic Tenant Migration on Startup**
- Create a `TenantMigrator` component that runs Flyway against all existing tenant schemas on app startup
- This ensures future migrations are automatically applied to all tenants

**Action Items:**
- [ ] Choose migration approach (A or B)
- [ ] Test migration on local database
- [ ] Verify `rfp_document_extractions` table exists in all tenant schemas
- [ ] Document chosen approach in CLAUDE.md

---

### 2. Test Document Extraction End-to-End
**Priority: HIGH**

Test the complete extraction flow with all supported file types.

**Test Cases:**
- [ ] Upload PDF file and verify extraction (text + page count)
- [ ] Upload DOCX file and verify extraction (text + tables)
- [ ] Upload XLSX file and verify extraction (text + tables with sheet names)
- [ ] Verify key-value extraction from document headers
- [ ] Test GET /api/documents/{id}/extraction endpoint
- [ ] Verify extraction status updates (PENDING → SUCCESS/FAILED)
- [ ] Test error handling (corrupted files, unsupported types)

**Files to Test With:**
- Sample RFP PDF
- Sample DOCX with tables
- Sample XLSX spreadsheet
- Document with key:value pairs in header

---

## Document Extraction - Future Enhancements

### 3. Move Extraction to Async/Background Processing
**Priority: MEDIUM**

Currently, extraction runs synchronously during upload. For large files, this can timeout.

**Implementation:**
- Add Spring `@Async` support or use a job queue (e.g., Redis-backed queue)
- Update document status: UPLOADED → PROCESSING → COMPLETED/FAILED
- Add polling endpoint for extraction status
- Consider: Spring Task Scheduler, AWS SQS, or RabbitMQ

**Files to Modify:**
- `DocumentExtractionService.java` - Add @Async annotation
- `RfpDocumentService.java` - Call extraction asynchronously
- `application.properties` - Configure thread pool

---

### 4. Improve PDF Table Extraction
**Priority: MEDIUM**

Current PDFBox implementation has limited table extraction. Consider upgrading to Azure Document Intelligence.

**Options:**
- **Azure Document Intelligence API** (from n8n workflow)
  - Better table detection and extraction
  - Form field recognition (key-value pairs)
  - Async API with polling
- **Tabula** (open-source alternative)
- **PDFBox with custom table detection logic**

**Decision Needed:**
- Cost/benefit analysis of Azure DI vs open-source
- Does RFP document quality justify cloud AI service?

---

### 5. Add OCR Support for Scanned Documents
**Priority: LOW**

Some RFP documents may be scanned PDFs without text layer.

**Options:**
- Tesseract OCR
- Azure Document Intelligence (includes OCR)
- Google Cloud Vision API

---

### 6. AI-Powered Structured Data Extraction
**Priority: HIGH (After testing basic extraction)**

Use OpenAI/Claude to extract structured RFP data from the extracted text.

**From n8n workflow analysis:**
- Extract insurance plan details (carrier, plan name, rates, deductibles, etc.)
- Map to structured schema (MED, DENT, VSN, STD, LTD, LIFE)
- Validate extracted data against source text (evidence matching)
- Generate QA metrics (presence %, evidence %)

**Implementation Plan:**
1. Design structured output schema (based on n8n CONFIG sheet)
2. Create prompt templates for different insurance types
3. Add OpenAI/Claude API integration
4. Store structured results in new table or extend extraction JSON
5. Add validation layer

**Files to Create:**
- `AiExtractionService.java`
- `dto/InsurancePlan.java` (and LOB-specific DTOs)
- New migration for structured data table

---

## Other Tasks

### 7. Add Swagger Documentation for Extraction Endpoint
**Priority: LOW**

Add OpenAPI annotations to the new extraction endpoint.

**Files to Modify:**
- `RfpDocumentController.java` - Add @Operation, @ApiResponse annotations

---

### 8. Add Unit Tests
**Priority: MEDIUM**

Test extraction logic with sample files.

**Test Files Needed:**
- `DocumentExtractionServiceTest.java`
- `KeyValueExtractionTest.java`
- Sample test documents in `src/test/resources/`

---

### 9. Update CLAUDE.md Documentation
**Priority: LOW**

Document the document extraction feature and migration process.

**Sections to Add:**
- Document Extraction architecture
- Supported file formats
- Extraction data model
- API endpoints for extraction
- Running tenant migrations

---

## Completed ✅

### Frontend Implementation (Jan 10, 2026)
- [x] Install Tailwind CSS v4 with PostCSS
- [x] Install and configure @clerk/nextjs
- [x] Create middleware.ts for route protection
- [x] Create sign-in/sign-up pages with Clerk components
- [x] Update layout.tsx with ClerkProvider, UserButton, OrganizationSwitcher
- [x] Create typed API client with JWT token injection (lib/api.ts, hooks/useApi.ts)
- [x] Create TypeScript types matching backend entities (types/api.ts)
- [x] Build UI component library (Button, Badge, Card, Alert, Skeleton, Modal, Tabs)
- [x] Update Documents page with auth and Tailwind styling
- [x] Update FileUpload component with Tailwind styling
- [x] Create Document detail page with extraction viewer
- [x] Create extraction viewer components (TextViewer, TablesViewer, KeyValuesViewer)
- [x] Create Dashboard with stats, quick actions, and recent documents

**Files Created**:
- `frontend/middleware.ts` - Route protection
- `frontend/app/sign-in/[[...sign-in]]/page.tsx` - Sign-in page
- `frontend/app/sign-up/[[...sign-up]]/page.tsx` - Sign-up page
- `frontend/types/api.ts` - TypeScript types
- `frontend/lib/api.ts` - API client factory
- `frontend/hooks/useApi.ts` - React hook for API calls
- `frontend/app/components/ui/*.tsx` - UI component library
- `frontend/app/components/extraction/*.tsx` - Extraction viewer components
- `frontend/app/documents/[id]/page.tsx` - Document detail page

**Files Modified**:
- `frontend/app/layout.tsx` - ClerkProvider, Navbar, globals.css import
- `frontend/app/page.tsx` - Dashboard with stats and quick actions
- `frontend/app/documents/page.tsx` - Document list with auth
- `frontend/app/components/FileUpload.tsx` - Tailwind styling, useApi hook
- `frontend/tsconfig.json` - Path aliases
- `frontend/.env.example` - Clerk configuration

---

### CORS Configuration (Jan 10, 2026)
- [x] Create WebConfig with CORS mappings for `/api/**`
- [x] Configure allowed origins from environment variable `CORS_ALLOWED_ORIGINS`
- [x] Set allowed methods (GET, POST, PUT, DELETE, OPTIONS, PATCH)
- [x] Enable credentials support for cross-origin requests
- [x] Add security headers interceptor (CSP, HSTS, X-Frame-Options, etc.)
- [x] Test CORS configuration (build and tests passing)

### Authentication Backend Implementation (Jan 9, 2026)
- [x] Add OAuth2 Resource Server dependency
- [x] Configure Clerk JWT validation via JWKS endpoint
- [x] Create ClerkPrincipal record for authenticated user info
- [x] Create ClerkAuthenticationToken for Spring Security
- [x] Create ClerkJwtAuthenticationConverter to extract org_slug from JWT
- [x] Create TenantAuthorizationFilter to set tenant context from auth
- [x] Create ApiKeyAuthenticationToken for service-to-service auth
- [x] Create ApiKeyAuthenticationFilter for X-API-Key header
- [x] Add lazy schema provisioning to OrgService
- [x] Update SecurityConfig for OAuth2 + API key auth
- [x] Update OpenApiConfig with Bearer auth scheme
- [x] Remove old TenantFilter (replaced by auth filters)

### Document Extraction Implementation (Dec 22, 2025)
- [x] Add PDFBox and POI dependencies to pom.xml
- [x] Create migration V3__document_extractions.sql
- [x] Create ExtractionStatus enum
- [x] Create RfpDocumentExtraction entity
- [x] Create RfpDocumentExtractionRepository
- [x] Create DTOs (ExtractionResult, ExtractedTable, KeyValuePair)
- [x] Add downloadFile method to FileStorageService
- [x] Create DocumentExtractionService with PDF/DOCX/XLSX extractors
- [x] Integrate extraction into RfpDocumentService.uploadDocument
- [x] Add GET /api/documents/{id}/extraction endpoint
- [x] Fix PDFBox 3.x API compatibility issue (Loader.loadPDF)
- [x] Fix Hibernate JSONB type mapping (@JdbcTypeCode annotation)
- [x] Add X-Tenant-Id header to all Swagger endpoints
- [x] Add @Parameter annotations to path variables for Swagger
- [x] Verify successful compilation and extraction working end-to-end

### S3 File Storage Migration (Dec 22, 2025)
- [x] **🔴 RESOLVED**: Migrate from local filesystem to AWS S3 storage
- [x] Add AWS SDK v2 dependency
- [x] Create S3Config with flexible credential provider
- [x] Rewrite FileStorageService for S3 (storeFile, deleteFile, fileExists, downloadFile)
- [x] Create FileStorageException for S3 errors
- [x] Update application.properties with S3 configuration
- [x] Add XLSX support to allowed content types
- [x] Update CLAUDE.md with S3 setup documentation

---

## Notes

### Known Limitations - Document Extraction
- PDF table extraction is basic (PDFBox has limited table detection)
- Extraction runs synchronously (may timeout on large files)
- No OCR support for scanned documents
- No AI-powered structured data extraction yet

### Known Limitations - Architecture (from ARCHITECTURE_REVIEW.md)
- **Authentication implemented**: Clerk JWT + API key auth (backend complete, frontend pending)
- **ThreadLocal tenant context**: Incompatible with @Async, virtual threads, reactive streams
- **Schema-per-tenant scalability**: Fine for <100 tenants, problematic at >500 tenants
  - Migration performance degrades with tenant count
  - Database catalog bloat in PostgreSQL metadata
  - Backup/restore complexity increases

### Dependencies Added
- Apache PDFBox 3.0.1
- Apache POI 5.2.5 (poi-ooxml)
- AWS SDK v2 for S3 (2.20.26)
- Spring Boot OAuth2 Resource Server (for Clerk JWT validation)

### Database Schema
- New table: `rfp_document_extractions` (per tenant schema)
- JSONB columns for flexible table/key-value storage
- Foreign key to `rfp_documents` with cascade delete

### Scalability Estimates (Schema-per-Tenant Pattern)
From ARCHITECTURE_REVIEW.md:
- **< 100 tenants**: No issues expected
- **100-500 tenants**: Migration performance concerns, monitor closely
- **> 500 tenants**: Serious operational challenges
  - Consider hybrid approach (shard to multiple DB instances)
  - Or migrate to row-level multitenancy pattern

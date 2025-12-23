# TODO - SaaS Starter JVM

> **Note**: See ARCHITECTURE_REVIEW.md for detailed security and scalability concerns.

## 🚨 Critical Security & Production Readiness

**These items MUST be completed before production deployment** (from Architecture Review)

### 🔴 CRITICAL - Authentication & Authorization
**Priority: CRITICAL | Blocks Production: YES | Effort: 1-2 weeks**

**Problem**: Currently, ANY client can access ANY tenant's data by simply setting the `X-Tenant-Id` header. There is NO verification that authenticated users belong to the tenant they're accessing.

**Attack Vector**:
```bash
# Attacker can access any tenant by changing header
curl -H "X-Tenant-Id: acme" http://api/documents
curl -H "X-Tenant-Id: competitor" http://api/documents  # 🚨 No validation!
```

**Required Implementation**:
- [ ] Implement OIDC/OAuth2 authentication (Spring Security)
- [ ] Store tenant membership in JWT claims or user profile
- [ ] Validate authenticated user's tenant matches `X-Tenant-Id` header in TenantFilter
- [ ] Return 403 Forbidden if tenant mismatch
- [ ] Consider deriving tenant from JWT claims instead of header

**Files to Create/Modify**:
- New: `config/SecurityConfig.java` (Spring Security config)
- New: `security/JwtAuthenticationFilter.java`
- New: `security/TenantAuthorizationFilter.java`
- Modify: `tenancy/TenantFilter.java` (add tenant validation)

**Decision Needed**:
- What's the authentication provider? (Auth0, Okta, Keycloak, custom JWT?)
- Single-tenant users or multi-tenant users?

---

### 🔴 CRITICAL - CORS Configuration
**Priority: CRITICAL | Blocks Production: YES | Effort: 1 hour**

**Problem**: Production frontend (different domain) won't be able to call backend API without CORS headers.

**Required Implementation**:
- [ ] Create WebConfig with CORS mapping for `/api/**`
- [ ] Configure allowed origins from environment variable
- [ ] Set allowCredentials for cookie/session support
- [ ] Add security headers (CSP, HSTS)

**Files to Create**:
- New: `config/WebConfig.java`

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(getAllowedOrigins())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

---

### 🟡 HIGH - Integration Tests (Multitenancy Isolation)
**Priority: HIGH | Blocks Production: YES | Effort: 2-3 weeks**

**Problem**: No automated tests means no safety net for refactoring. Critical to verify tenant data isolation.

**Required Test Coverage**:
- [ ] **Multitenancy isolation tests** - Ensure tenant A can't access tenant B data
- [ ] Org creation + schema provisioning flow
- [ ] Flyway migrations on tenant schemas
- [ ] File upload/download with S3
- [ ] Document extraction with PDF/DOCX/XLSX samples
- [ ] API endpoint contracts (OpenAPI validation)

**Files to Create**:
- `src/test/java/com/acme/saas/MultitenancyIsolationTest.java`
- `src/test/java/com/acme/saas/OrgProvisioningTest.java`
- `src/test/java/com/acme/saas/DocumentExtractionTest.java`
- `src/test/resources/` - Sample test documents

**Tools**: Testcontainers (PostgreSQL), JUnit 5, Spring Boot Test

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
- **No authentication**: Any client can access any tenant data by changing X-Tenant-Id header 🚨
- **ThreadLocal tenant context**: Incompatible with @Async, virtual threads, reactive streams
- **Schema-per-tenant scalability**: Fine for <100 tenants, problematic at >500 tenants
  - Migration performance degrades with tenant count
  - Database catalog bloat in PostgreSQL metadata
  - Backup/restore complexity increases

### Dependencies Added
- Apache PDFBox 3.0.1
- Apache POI 5.2.5 (poi-ooxml)
- AWS SDK v2 for S3 (2.20.26)

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

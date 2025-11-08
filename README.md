# SaaS Starter (JVM-first)

Monorepo with **Spring Boot 3 (Java 21)** backend and **Next.js + TypeScript** frontend.
- Multitenancy (schema-per-tenant) scaffold
- OpenAPI, Stripe-ready stubs, Auth (OIDC) placeholders
- Docker Compose for Postgres + Redis
- GitHub Actions CI (build + test)

## Quick start

### Prereqs
- Java 21, Docker, pnpm, Node 20+, Git

### Dev
```bash
# Terminal 1: Start databases
docker compose up -d

# Terminal 1: Build and run backend
cd backend
export PATH="/opt/homebrew/bin:$PATH"  # If Maven not in PATH
mvn clean install -DskipTests
mvn spring-boot:run

# Terminal 2: Run frontend
cd frontend
pnpm install  # Only needed first time
pnpm dev
```

Backend runs at http://localhost:8080
Frontend runs at http://localhost:3000

### Env
Copy `.env.example` files and set secrets.

### Multitenancy
- Header **X-Tenant-Id** selects the tenant schema (e.g., `acme` -> schema `tenant_acme`).
- `POST /api/orgs` creates an org and its schema/migrations via Flyway.

### Build & Test
```bash
cd backend && mvn clean test
cd frontend && pnpm build
```

### Deploy
- Containerize backend (Dockerfile included) and run on ECS/EKS/Fargate.
- Frontend can be deployed to Vercel or any Node host.

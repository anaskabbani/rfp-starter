# Setup Instructions

## Prerequisites

You need to install the following before running the project:

### 1. Java 21
The backend requires Java 21. Install using one of these methods:

**Option A: Using Homebrew (Recommended)**
```bash
brew install openjdk@21
```

**Option B: Using SDKMAN**
```bash
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.1-tem
```

**Option C: Download from Oracle/Adoptium**
- Visit: https://adoptium.net/temurin/releases/?version=21
- Download macOS installer
- Install and set JAVA_HOME

After installing, verify:
```bash
java -version
# Should show: openjdk version "21.x.x"
```

### 2. Maven
Install Maven to build the backend:

**Using Homebrew:**
```bash
brew install maven
```

Verify:
```bash
mvn -version
```

### 3. Docker & Docker Compose
Install Docker Desktop for macOS:
- Download from: https://www.docker.com/products/docker-desktop/
- Install and start Docker Desktop

Verify:
```bash
docker --version
docker compose version
```

### 4. Node.js & pnpm (Already Installed ✅)
- Node.js: v20.17.0 ✅
- pnpm: v10.19.0 ✅

## Running the Project

### Step 1: Start Database Services
```bash
cd /Users/anaskabbani/Projects/mosaic/saas-starter-jvm
docker compose up -d
```

This starts:
- PostgreSQL on port 5432
- Redis on port 6379

### Step 2: Build and Run Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will:
- Start on http://localhost:8080
- Run database migrations automatically
- Create storage directory for uploaded files

### Step 3: Run Frontend (Already Done ✅)
```bash
cd frontend
pnpm dev
```

The frontend will:
- Start on http://localhost:3000
- Hot reload on file changes

## Access Points

- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080
- **API Docs (Swagger):** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/health

## Testing the File Upload

1. Navigate to http://localhost:3000/documents
2. Upload a PDF, DOCX, DOC, or TXT file (max 50MB)
3. Files are stored in `./storage/tenant_<id>/` directory

## Troubleshooting

### Java Not Found
- Make sure Java 21 is installed
- Set JAVA_HOME: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`
- Add to PATH: `export PATH=$JAVA_HOME/bin:$PATH`

### Maven Not Found
- Install Maven: `brew install maven`
- Verify: `mvn -version`

### Docker Not Running
- Start Docker Desktop
- Verify: `docker ps`

### Database Connection Error
- Make sure Docker containers are running: `docker compose ps`
- Check logs: `docker compose logs db`

### Port Already in Use
- Backend (8080): Change in `application.yml`
- Frontend (3000): Change in `package.json` scripts
- Database (5432): Change in `docker-compose.yml`

## Next Steps

Once everything is running:
1. Create an organization: `POST /api/orgs` with `{"slug": "acme", "name": "Acme Inc"}`
2. Upload documents via the frontend
3. Check API docs at http://localhost:8080/swagger-ui.html


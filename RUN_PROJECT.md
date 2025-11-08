# How to Run the Project

## Prerequisites

Before running the project, ensure you have the following installed:

1. **Java 21** - Required for the backend
   ```bash
   java -version
   # Should show: openjdk version "21.x.x"
   ```

2. **Maven** - Build tool for the backend
   ```bash
   mvn -version
   # Should show: Apache Maven 3.x.x
   ```

3. **Node.js 20+** - Required for the frontend
   ```bash
   node --version
   # Should show: v20.x.x or higher
   ```

4. **pnpm** - Package manager for the frontend
   ```bash
   pnpm --version
   # Should show: v10.x.x or higher
   ```

5. **Docker & Docker Compose** - Required for database and Redis
   ```bash
   docker --version
   docker compose version
   ```

## Step-by-Step Instructions

### Step 1: Start Database Services

Start PostgreSQL and Redis using Docker Compose:

```bash
cd /Users/anaskabbani/Projects/mosaic/saas-starter-jvm
docker compose up -d
```

Verify services are running:
```bash
docker compose ps
```

You should see:
- `saas-starter-jvm-db-1` (PostgreSQL) - running on port 5432
- `saas-starter-jvm-redis-1` (Redis) - running on port 6379

### Step 2: Build the Backend

Navigate to the backend directory and build the project:

```bash
cd backend
export PATH="/opt/homebrew/bin:$PATH"  # Only needed if Maven is not in PATH
mvn clean install -DskipTests
```

**Note:** If Maven is not in your PATH, add it:
```bash
# For Homebrew installations:
export PATH="/opt/homebrew/bin:$PATH"

# Or add to ~/.zshrc permanently:
echo 'export PATH="/opt/homebrew/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### Step 3: Run the Backend

Start the Spring Boot application:

```bash
cd backend
export PATH="/opt/homebrew/bin:$PATH"  # Only needed if Maven is not in PATH
mvn spring-boot:run
```

The backend will:
- Start on **http://localhost:8080**
- Run database migrations automatically
- Create storage directory for uploaded files

**Wait for:** You should see `Started Application` in the logs

**Verify it's running:**
```bash
curl http://localhost:8080/health
# Should return: {"status":"ok"}
```

### Step 4: Install Frontend Dependencies

Open a **new terminal window** and navigate to the frontend:

```bash
cd /Users/anaskabbani/Projects/mosaic/saas-starter-jvm/frontend
pnpm install
```

This only needs to be done once (or when dependencies change).

### Step 5: Run the Frontend

Start the Next.js development server:

```bash
cd frontend
pnpm dev
```

The frontend will:
- Start on **http://localhost:3000**
- Hot reload on file changes

**Verify it's running:**
Open your browser and go to: http://localhost:3000

## Access Points

Once everything is running:

- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080
- **API Documentation (Swagger):** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/health
- **Documents Page:** http://localhost:3000/documents

## Quick Start (All Commands)

If you want to run everything in sequence:

```bash
# Terminal 1: Start databases
cd /Users/anaskabbani/Projects/mosaic/saas-starter-jvm
docker compose up -d

# Terminal 1: Build and run backend
cd backend
export PATH="/opt/homebrew/bin:$PATH"
mvn clean install -DskipTests
mvn spring-boot:run

# Terminal 2: Run frontend
cd /Users/anaskabbani/Projects/mosaic/saas-starter-jvm/frontend
pnpm install  # Only needed first time
pnpm dev
```

## Testing the Application

### 1. Create an Organization (Optional)

```bash
curl -X POST http://localhost:8080/api/orgs \
  -H "Content-Type: application/json" \
  -d '{"slug":"acme","name":"Acme Inc"}'
```

### 2. Upload a Document

1. Navigate to http://localhost:3000/documents
2. Drag & drop a file or click to browse
3. Supported formats: PDF, DOCX, DOC, TXT (max 50MB)
4. File will be stored in `./storage/tenant_<id>/` directory

### 3. View API Documentation

Visit http://localhost:8080/swagger-ui.html to explore all available endpoints.

## Troubleshooting

### Backend won't start

**Issue:** Port 8080 already in use
```bash
# Find what's using port 8080
lsof -ti:8080

# Kill the process (replace PID with actual process ID)
kill -9 <PID>

# Or change port in application.properties:
# server.port=8081
```

**Issue:** Database connection error
```bash
# Check if PostgreSQL is running
docker compose ps

# Check database logs
docker compose logs db

# Restart database
docker compose restart db
```

**Issue:** Flyway migration error
- Make sure PostgreSQL is running
- Check database credentials in `application.properties`
- Verify migrations exist in `src/main/resources/db/migration/`

### Frontend won't start

**Issue:** Port 3000 already in use
```bash
# Find what's using port 3000
lsof -ti:3000

# Kill the process
kill -9 <PID>

# Or change port in package.json scripts:
# "dev": "next dev -p 3001"
```

**Issue:** Dependencies not installed
```bash
cd frontend
rm -rf node_modules
pnpm install
```

### Database issues

**Reset database:**
```bash
docker compose down -v  # Removes volumes (deletes all data)
docker compose up -d     # Recreates containers
```

**View database logs:**
```bash
docker compose logs db
```

**Connect to database:**
```bash
docker compose exec db psql -U app -d app
```

## Stopping the Project

### Stop Frontend
Press `Ctrl+C` in the frontend terminal

### Stop Backend
Press `Ctrl+C` in the backend terminal

### Stop Database Services
```bash
docker compose down
```

### Stop Everything
```bash
# Stop all Docker containers
docker compose down

# Stop backend (Ctrl+C in terminal)
# Stop frontend (Ctrl+C in terminal)
```

## Environment Variables

You can override configuration using environment variables:

**Backend:**
- `DB_URL` - Database connection URL
- `DB_USER` - Database username
- `DB_PASS` - Database password
- `STORAGE_LOCATION` - File storage location

**Frontend:**
- `NEXT_PUBLIC_API_BASE` - Backend API URL (default: http://localhost:8080)

Example:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/mydb
export DB_USER=myuser
export DB_PASS=mypass
mvn spring-boot:run
```

## Next Steps

Once the project is running:
1. ✅ Upload RFP documents via the frontend
2. ✅ View uploaded documents
3. ✅ Explore API endpoints via Swagger UI
4. 🔄 Add AI integration for document processing (coming next)
5. 🔄 Add background job processing (coming next)

## Need Help?

- Check logs in the terminal where services are running
- View API documentation at http://localhost:8080/swagger-ui.html
- Check Docker logs: `docker compose logs`
- Review configuration in `backend/src/main/resources/application.properties`


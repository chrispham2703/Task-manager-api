# Task Manager API

A **production-grade** RESTful API for task management built with Spring Boot 3.x, featuring JWT authentication, role-based access control, and comprehensive task ownership enforcement.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)
[![CI](https://github.com/chrispham2703/Task-manager-api/actions/workflows/ci.yml/badge.svg)](https://github.com/chrispham2703/Task-manager-api/actions/workflows/ci.yml)
## 🌟 Features

### Authentication & Security
- **JWT-based stateless authentication** with configurable expiration
- **Refresh token** support for seamless session management
- **BCrypt password hashing** (cost factor 10)
- **Role-based access control** (USER, ADMIN)
- **Rate limiting** to prevent abuse

### User Management
- UUID primary keys (non-sequential for security)
- Case-insensitive unique email addresses
- Soft-delete support (ACTIVE, LOCKED, DELETED statuses)
- Password never exposed in API responses

### Task Management
- Full CRUD operations
- **Ownership enforcement** at service layer
- Cross-user isolation (secure 404 response)
- Pagination, filtering, and sorting
- Status workflow (TODO → IN_PROGRESS → DONE); soft-deleted tasks marked as DELETED
- Priority levels (LOW, MEDIUM, HIGH)
- **Soft-delete** for tasks (DELETE endpoint marks task as DELETED; data preserved but excluded from queries)

### Infrastructure
- **Flyway** database migrations (no auto-DDL)
- **Docker Compose** for local development (DB + App with health checks)
- **Multi-stage Dockerfile** for optimized production images
- **Spring Actuator** for health checks and metrics
- **OpenAPI/Swagger** documentation
- **CORS** configured and environment-driven
- Environment-based configuration (12-factor app)

### CI/CD Pipeline (GitHub Actions)
- **Unit tests** with JaCoCo coverage reports
- **Docker image build** verification
- **API integration tests** via Newman (Postman CLI) against real Docker stack
- **OWASP Dependency-Check** for CVE scanning

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### 1. Clone the repository
```bash
git clone https://github.com/chrispham2703/Task-manager-api.git
cd Task-manager-api
```

### 2. Start PostgreSQL
```bash
docker compose up -d
```

### 3. Configure application
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Option 1: Edit `application.properties`
# Option 2: Export env vars (see `.env.example`) and keep `application.properties` as-is
```

### 4. Run the application
```bash
./mvnw spring-boot:run
```

### 5. Access the API
- **API Base URL**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

## 📬 API Collection

A complete [Postman collection](postman/Task-Manager-API.postman_collection.json) is included for testing and demo purposes.

### Setup
1. Import `postman/Task-Manager-API.postman_collection.json` into Postman
2. Import `postman/Task-Manager-API.postman_environment.json` as environment
3. Select **"Task Manager API - Local"** environment
4. Make sure the API is running at `http://localhost:8080`

### Folders

| Folder | Purpose | How to run |
|--------|---------|------------|
| **1. Demo Flow** | Full auth → CRUD → ownership isolation → refresh → cleanup | Collection Runner, run in order |
| **2. Manual Operations** | Standalone requests with auto-saved variables | Run individually |
| **3. Negative Cases** | Validates 401/400/404 error handling | Run individually or with Runner |

### Variables

| Variable | Description | Set by |
|----------|-------------|--------|
| `baseUrl` | API base URL | Environment (default: `http://localhost:8080`) |
| `accessToken` | JWT access token | Auto-saved after login/register |
| `refreshToken` | JWT refresh token | Auto-saved after login/register |
| `taskId` | Last created task ID | Auto-saved after create |
| `taskIdA` | User A's task ID (for ownership tests) | Auto-saved in Demo Flow |
| `selectedTaskId` | Task to operate on in Manual Operations | Auto-saved from Get All Tasks |
| `selectedTaskIndex` | Which task to select from list (0-based) | Set manually (default: `0`) |

## 📚 API Documentation

### Authentication Endpoints

#### Register a new user
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "status": "ACTIVE",
    "roles": ["USER"],
    "createdAt": "2026-01-23T10:00:00Z"
  }
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

#### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

### User Endpoints

#### Get current user
```http
GET /api/users/me
Authorization: Bearer {accessToken}
```

### Task Endpoints

#### Create a task
```http
POST /api/tasks
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "title": "Complete project documentation",
  "description": "Write comprehensive README and API docs",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-02-01"
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "ownerId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Complete project documentation",
  "description": "Write comprehensive README and API docs",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-02-01",
  "createdAt": "2026-01-23T10:00:00Z",
  "updatedAt": "2026-01-23T10:00:00Z"
}
```

#### Get all tasks (with pagination & filtering)
```http
GET /api/tasks?page=0&size=10&status=TODO&priority=HIGH&sort=dueDate,asc
Authorization: Bearer {accessToken}
```

#### Get a specific task
```http
GET /api/tasks/{id}
Authorization: Bearer {accessToken}
```

#### Update a task
```http
PUT /api/tasks/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "title": "Updated title",
  "status": "IN_PROGRESS"
}
```

#### Delete a task
```http
DELETE /api/tasks/{id}
Authorization: Bearer {accessToken}
```

**Response:** `204 No Content`

## 🔒 Security

### Authentication Flow
1. User registers or logs in → receives `accessToken` + `refreshToken`
2. Include `Authorization: Bearer {accessToken}` in subsequent requests
3. When `accessToken` expires, use `refreshToken` to get a new pair
4. `refreshToken` has longer validity (7 days default)

### Ownership Enforcement
- Users can only access their own tasks
- Attempting to access another user's task returns `404 Not Found` (not `403`) for security
- Task ownership is enforced at the service layer

### Rate Limiting
- **Authentication endpoints**: 10 requests/minute per IP
- **API endpoints**: 100 requests/minute per user

## 🗄️ Database Schema

### Users Table
```sql
CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email         VARCHAR(255) NOT NULL,
  password_hash VARCHAR(72)  NOT NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));
```

### Tasks Table
```sql
CREATE TABLE tasks (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title       VARCHAR(255) NOT NULL,
  description TEXT,
  status      VARCHAR(20)  NOT NULL,
  priority    VARCHAR(20)  NOT NULL,
  due_date    DATE,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_tasks_owner_id ON tasks (owner_id);
CREATE INDEX ix_tasks_owner_id_status ON tasks (owner_id, status);
CREATE INDEX ix_tasks_owner_id_due_date ON tasks (owner_id, due_date);
```

## 🧪 Testing

### Run all tests
```bash
./mvnw test
```

### Run with coverage
```bash
./mvnw clean verify
# JaCoCo report generated at target/site/jacoco/index.html
```

### Run full stack (Docker)
```bash
docker compose up -d --build
# App: http://localhost:8080  |  Swagger: http://localhost:8080/swagger-ui.html
# Postgres: localhost:5433

# Run API tests
newman run postman/Task-Manager-API.postman_collection.json \
  -e postman/Task-Manager-API.postman_environment.json \
  --delay-request 200
```

## 📁 Project Structure

```
src/main/java/com/taskmanager/api/
├── common/
│   ├── AuditableEntity.java          # Base entity with createdAt/updatedAt
│   └── exception/
│       ├── ErrorResponse.java        # Consistent error format
│       ├── GlobalExceptionHandler.java
│       └── ResourceNotFoundException.java
├── config/
│   ├── JpaAuditingConfig.java
│   ├── OpenApiConfig.java            # Swagger configuration
│   └── RateLimitConfig.java          # Rate limiting configuration
├── security/
│   ├── AuthenticatedUser.java        # UserDetails implementation
│   ├── JwtAuthFilter.java            # JWT authentication filter
│   ├── JwtUtil.java                  # JWT utility class
│   └── SecurityConfig.java           # Security configuration
├── user/
│   ├── User.java                     # User entity
│   ├── UserRepository.java
│   ├── UserService.java
│   ├── UserController.java
│   ├── AuthController.java           # Login/Register endpoints
│   └── [DTOs]
├── task/
│   ├── Task.java                     # Task entity
│   ├── TaskRepository.java
│   ├── TaskService.java
│   ├── TaskController.java
│   └── [DTOs]
└── TaskManagerApiApplication.java
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/task_manager` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | JWT signing key (min 256 bits) | - |
| `JWT_EXPIRATION_MS` | Access token validity (ms) | `86400000` (24h) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token validity (ms) | `604800000` (7d) |

## ⚠️ Error Responses

All error responses follow a consistent JSON format:

### 400 Validation Error
```json
{
  "timestamp": "2026-01-23T10:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "path": "/api/tasks",
  "validationErrors": {
    "title": "Title is required",
    "email": "Invalid email format"
  }
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2026-01-23T10:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "path": "/api/auth/login"
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-01-23T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Task not found",
  "path": "/api/tasks/550e8400-e29b-41d4-a716-446655440001"
}
```

### 429 Too Many Requests
```json
{
  "error": "Too many requests",
  "message": "Rate limit exceeded. Please try again later."
}
```

## 🏗️ Engineering Decisions

| Decision | Rationale |
|----------|-----------|
| **UUID primary keys** | Non-sequential IDs prevent enumeration attacks and information leakage about entity count/creation order |
| **404 instead of 403 for ownership** | Returning 403 confirms the resource exists; 404 reveals nothing, preventing resource discovery by unauthorized users |
| **Flyway over auto-DDL** | Explicit, versioned migrations ensure reproducible schema changes across environments and enable safe rollbacks |
| **Soft delete for users and tasks** | Preserves data for audit trails and recovery; users marked DELETED cannot authenticate; tasks marked DELETED are excluded from all queries |
| **JWT + refresh token** | Stateless access tokens reduce DB lookups per request; refresh tokens allow long sessions without long-lived access tokens; only access tokens are accepted for API calls |
| **BCrypt (cost 10)** | Industry-standard adaptive hashing; cost factor 10 balances security and latency for auth endpoints |
| **Rate limiting per IP/user** | Auth endpoints (10 req/min) prevent credential stuffing; API endpoints (100 req/min) prevent abuse while allowing normal usage |
| **H2 for unit tests, Postgres for integration** | Fast test cycles locally; Newman + Docker Compose validates real DB behavior in CI |
| **Multi-stage Docker build** | Separate build/runtime stages keep production image small (~200 MB JRE vs ~800 MB Maven SDK) |
| **CORS configurable via properties** | `cors.allowed-origins` env var allows different origins per environment without code changes |

## ⚖️ Trade-offs & Known Limitations

| Area | Current approach | Trade-off |
|------|-----------------|-----------|
| **Token revocation** | JWT is stateless — no server-side revocation | If a refresh token leaks, it remains valid until expiry. Production fix: add a token blocklist (Redis) or short-lived access + DB-backed refresh |
| **Rate limit key** | IP for auth, `Authorization` hash for API | Users behind shared NAT hit the same bucket; hash of auth header changes on token refresh. Better: rate-limit by `userId` from JWT claims |
| **H2 vs Postgres in tests** | Unit tests use H2 with `MODE=PostgreSQL` | Some Postgres-specific behavior (e.g. `gen_random_uuid()`, partial indexes) is not tested in unit tests. Mitigated by Newman integration tests against real Postgres |
| **No HTTPS in app** | Relies on reverse proxy / load balancer for TLS | App itself serves plain HTTP. Standard in container deployments (TLS terminates at ingress) but must be documented |
| **ADMIN role** | Data model supports it, barely enforced | Only `GET /users/{id}` checks for ADMIN. Future: admin dashboard, user management endpoints |
| **Soft-delete data growth** | Deleted records stay in DB forever | No cleanup/archive job. Production: add scheduled purge or move to archive table |

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 👨‍💻 Author

**Chris Pham**
- GitHub: [@chrispham2703](https://github.com/chrispham2703)

---

⭐ If you found this project helpful, please give it a star!

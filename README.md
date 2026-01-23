# Task Manager API

A **production-grade** RESTful API for task management built with Spring Boot 3.x, featuring JWT authentication, role-based access control, and comprehensive task ownership enforcement.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

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
- Status workflow (TODO → IN_PROGRESS → DONE)
- Priority levels (LOW, MEDIUM, HIGH)

### Infrastructure
- **Flyway** database migrations (no auto-DDL)
- **Docker Compose** for local development
- **Spring Actuator** for health checks and metrics
- **OpenAPI/Swagger** documentation
- Environment-based configuration

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
# Edit application.properties with your settings
```

### 4. Run the application
```bash
./mvnw spring-boot:run
```

### 5. Access the API
- **API Base URL**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

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
  title       VARCHAR(120) NOT NULL,
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
./mvnw test jacoco:report
# Report available at target/site/jacoco/index.html
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

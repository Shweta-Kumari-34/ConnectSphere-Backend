# 🌐 ConnectSphere

**A microservices-based social media platform** built with Spring Boot, Angular, Kafka, Redis, RabbitMQ, and MySQL.

---

## 📋 Architecture Overview

```
┌──────────────────────────────────────────────┐
│           Angular Frontend (4200)            │
└──────────────────┬───────────────────────────┘
                   │
┌──────────────────▼───────────────────────────┐
│         API Gateway — Spring Cloud (8090)     │
│         JWT Validation · Route Balancing      │
└──────────────────┬───────────────────────────┘
                   │
    ┌──────────────┼──────────────────┐
    │              │                  │
┌───▼───┐    ┌────▼────┐     ┌───────▼───────┐
│ Auth  │    │  Post   │     │  13 Services  │
│ 8083  │    │  8082   │     │   + Eureka    │
└───┬───┘    └────┬────┘     └───────┬───────┘
    │             │                  │
    ▼             ▼                  ▼
┌──────────────────────────────────────────────┐
│  MySQL 8.0 · Redis · Kafka · RabbitMQ        │
└──────────────────────────────────────────────┘
```

### Microservices

| Service | Port | Description |
|---------|------|-------------|
| **Eureka Server** | 8761 | Service Discovery |
| **API Gateway** | 8090 | Centralized Routing + JWT |
| **Auth Service** | 8083 | Authentication, OAuth2, Profile |
| **Post Service** | 8082 | Posts, Reels, Feed |
| **Comment Service** | 8093 | Comments & Replies |
| **Like Service** | 8084 | Likes & Reactions |
| **Follow Service** | 8085 | Social Graph |
| **Notification Service** | 8086 | Real-time Alerts |
| **Media Service** | 8092 | File Upload (Cloudinary) |
| **Search Service** | 8088 | User & Content Discovery |
| **Payment Service** | 8089 | Subscriptions & Payments |
| **Email Service** | 8087 | Async Email Delivery |
| **Admin Server** | 9090 | Spring Boot Admin Dashboard |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | Angular 21, TypeScript, SCSS |
| **Backend** | Java 17, Spring Boot 3, Spring Cloud |
| **Gateway** | Spring Cloud Gateway (Reactive) |
| **Database** | MySQL 8.0 (per-service schema) |
| **Cache** | Redis 7 |
| **Messaging** | Apache Kafka, RabbitMQ |
| **Storage** | Cloudinary (media), Local FS (fallback) |
| **Discovery** | Netflix Eureka |
| **Auth** | JWT (HMAC-SHA256), Google OAuth2, OTP |
| **Docs** | Swagger / OpenAPI 3 |
| **CI/CD** | Jenkins, Docker, SonarQube |
| **Testing** | JUnit 5, Mockito, JaCoCo, Vitest |

---

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Node.js 20+
- MySQL 8.0
- Docker & Docker Compose (optional)

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/your-username/ConnectSphere.git
cd ConnectSphere

# Copy environment file and fill in your values
cp .env.example .env

# Start everything
docker-compose up --build
```

| URL | Service |
|-----|---------|
| http://localhost:4200 | Frontend |
| http://localhost:8090 | API Gateway |
| http://localhost:8761 | Eureka Dashboard |
| http://localhost:9090 | Admin Dashboard |

### Option 2: Local Development

```bash
# 1. Set up environment
cp .env.example .env
# Edit .env with your local values

# 2. Start infrastructure (MySQL, Kafka, Redis, RabbitMQ)
# Ensure MySQL is running on port 3306

# 3. Start all services
start-all.bat

# 4. Start frontend
cd ConnectSphere-Frontend
npm install
npx ng serve --open
```

---

## ⚙️ Environment Variables

All sensitive configuration is externalized via environment variables. See [`.env.example`](.env.example) for the full list.

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_USERNAME` | Yes | MySQL username |
| `DB_PASSWORD` | Yes | MySQL password |
| `JWT_SECRET` | Yes | JWT signing key (min 64 chars) |
| `CLOUDINARY_CLOUD_NAME` | Yes | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Yes | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Yes | Cloudinary API secret |
| `GMAIL_USERNAME` | No | Gmail address for SMTP |
| `GMAIL_APP_PASSWORD` | No | Gmail app password |
| `GOOGLE_CLIENT_ID` | No | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | No | Google OAuth2 client secret |

---

## 📁 Project Structure

```
ConnectSphere/
├── ConnectSphere-Backend/        # Java microservices
│   ├── eureka-server/            # Service discovery
│   ├── api-gateway/              # API gateway + JWT filter
│   ├── auth-service/             # Authentication
│   ├── post-service/             # Posts & Reels
│   ├── comment-service/          # Comments
│   ├── like-service/             # Likes
│   ├── follow-service/           # Social graph
│   ├── notification-service/     # Notifications
│   ├── media-service/            # Media upload
│   ├── search-service/           # Search
│   ├── payment-service/          # Payments
│   ├── email-service/            # Email delivery
│   └── admin-server/             # Monitoring
├── ConnectSphere-Frontend/       # Angular SPA
├── docker-compose.yml            # Full-stack orchestration
├── Jenkinsfile                   # CI/CD pipeline
├── .env.example                  # Environment template
└── init-databases.sql            # DB initialization
```

---

## 📖 Documentation

- [Architecture Design](CONNECTSPHERE_ARCHITECTURE_DESIGN.md) — Full system design & patterns
- [API Reference](CONNECTSPHERE_API_REFERENCE.md) — REST endpoint documentation
- [Swagger UI](http://localhost:8090/swagger-ui.html) — Interactive API explorer (when running)

---

## 🧪 Testing

```bash
# Run tests for a specific service
cd ConnectSphere-Backend/auth-service/auth-service
./mvnw test

# Run with coverage report
./mvnw test jacoco:report
```

---

## 📄 License

This project is developed as part of an academic curriculum.

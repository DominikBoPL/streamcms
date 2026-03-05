# StreamCMS 🎬

A self-hosted content platform combining VOD streaming, live broadcasting,
and a full CMS — built as a portfolio project to demonstrate modern
backend architecture.

## Architecture
```
FRONTEND (Next.js) → API PROXY (Java + Micronaut) → CORE (Kotlin + Spring Boot)
```

- **Core** — business logic, media processing, database, event publishing
- **API Proxy** — authentication, rate limiting, request routing
- **Frontend** — video player, live stream, blog, admin panel

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Core services | Kotlin, Spring Boot 3, Spring Data JPA |
| API Gateway | Java, Micronaut |
| Frontend | Next.js 14, TypeScript, Tailwind CSS |
| Database | PostgreSQL |
| Object Storage | MinIO (S3-compatible) |
| Messaging | RabbitMQ |
| Auth | Keycloak (OAuth2 / JWT) |
| Video Processing | FFmpeg, Nginx-RTMP, HLS |
| Monitoring | Grafana, Prometheus |
| Infrastructure | Docker, Docker Compose |

## Running Locally

> Full setup guide coming as the project develops.

### Prerequisites
- Docker + Docker Compose
- Java 21
- Node.js 20+

### Start infrastructure
```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

### Start core services
```bash
./gradlew :core:media-service:bootRun
```

## Project Status

🚧 **In active development** — built step by step as a learning project.

| Service | Status |
|---------|--------|
| media-service | 🔨 In progress |
| cms-service | 📋 Planned |
| live-service | 📋 Planned |
| billing-service | 📋 Planned |
| api-proxy | 📋 Planned |
| frontend | 📋 Planned |

## License
MIT

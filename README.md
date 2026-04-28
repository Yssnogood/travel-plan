# Travel-Plan - Travel Management System

## 🏗️ Architecture Overview

This project implements a **microservices architecture** for a Travel Management System with an Admin Dashboard.

### Microservices

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Kong-based gateway for routing and load balancing |
| Auth Service | 8081 | Authentication & Authorization (JWT, OAuth2) |
| User Service | 8082 | User management CRUD operations |
| Travel Service | 8083 | Travel, destinations, activities management |
| Payment Service | 8084 | Stripe & PayPal integration |
| Notification Service | 8085 | Email/SMS notifications |

### Infrastructure Components

| Component | Port | Description |
|-----------|------|-------------|
| PostgreSQL | 5432 | Primary relational database |
| Neo4j | 7474/7687 | Graph database for travel relationships |
| Redis | 6379 | Caching and session management |
| Jenkins | 8090 | CI/CD pipeline |
| SonarQube | 9000 | Code quality analysis |
| Vault | 8200 | Secret management |
| Elasticsearch | 9200 | Log aggregation |
| Kibana | 5601 | Log visualization |
| Jaeger | 16686 | Distributed tracing |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Metrics visualization |

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Node.js 18+
- Maven 3.8+

### 1. Environment Configuration

```bash
# Copy the example environment file and fill in your values
cp .env.example .env
```

Edit `.env` with your credentials (JWT secret, DB passwords, Stripe/PayPal keys, SMTP settings).  
The application reads this file automatically via Docker Compose.

### 2. Start Infrastructure

```bash
# Core infrastructure (PostgreSQL, Neo4j, Redis, RabbitMQ, Vault, Kong)
docker-compose -f docker/docker-compose.infra.yml up -d

# Monitoring stack (Prometheus, Grafana, Elasticsearch, Kibana, Jaeger)
docker-compose -f docker/docker-compose.monitoring.yml up -d
```

### 3. Seed Initial Data

```bash
# Create the default ADMIN user (required before first login)
# Windows:
powershell -ExecutionPolicy Bypass -File scripts\ensure-admin.ps1

# Seed demo users and travel data (optional)
powershell -ExecutionPolicy Bypass -File scripts\seed-users.ps1
```

### 4. Start Microservices

```bash
docker-compose -f docker/docker-compose.services.yml up -d
```

### 5. Start Admin Dashboard

```bash
cd admin-dashboard
npm install
npm run dev
```

The dashboard is available at `http://localhost:5173` (Vite default).

### 6. Verify — Smoke Tests

```bash
# Linux/macOS:
bash scripts/smoke-tests.sh

# Windows:
powershell -ExecutionPolicy Bypass -File scripts\smoke-tests.ps1
```

Expected output: all health and reachability checks `OK`.

---

### Using Ansible (production deployment)

```bash
# Deploy entire stack
cd ansible
ansible-playbook -i inventory/hosts.yml playbooks/deploy-all.yml

# Deploy specific service
ansible-playbook -i inventory/hosts.yml playbooks/deploy-service.yml -e "service=user-service"
```

> Ansible idempotence evidence (dual-run + report): see [docs/ansible-evidence.md](docs/ansible-evidence.md) (available after merging `feat/ansible-audit-evidence`).

## 📁 Project Structure

```
travel-plan/
├── docker/                     # Docker configurations
│   ├── docker-compose.infra.yml
│   ├── docker-compose.services.yml
│   └── docker-compose.monitoring.yml
├── ansible/                    # Ansible playbooks
│   ├── inventory/
│   ├── playbooks/
│   └── roles/
├── jenkins/                    # Jenkins pipeline configs
├── services/                   # Microservices
│   ├── api-gateway/
│   ├── auth-service/
│   ├── user-service/
│   ├── travel-service/
│   ├── payment-service/
│   └── notification-service/
├── admin-dashboard/            # React Admin Dashboard
├── shared/                     # Shared libraries
├── scripts/                    # Utility scripts
└── docs/                       # Documentation
```

## 🔐 Security Features

- **SSL/TLS**: All services communicate over HTTPS
- **JWT Authentication**: Stateless token-based auth
- **Role-Based Access Control**: Admin, Manager, User roles
- **HashiCorp Vault**: Secure secret management
- **Network Isolation**: Services accessible only via API Gateway
- **Principle of Least Privilege**: Minimal permissions per service

## 👮 Authorization Matrix (RBAC)

The following policy is enforced for audit-critical CRUD endpoints:

| Domain | Endpoint Pattern | Methods | Required Role |
|--------|------------------|---------|---------------|
| Users | `/api/v1/users` | `GET`, `POST` | `ADMIN` |
| Users | `/api/v1/users/{id}` | `GET`, `PUT`, `DELETE` | `ADMIN` |
| Travels | `/api/v1/travels` | `GET`, `POST` | `ADMIN` |
| Travels | `/api/v1/travels/{id}` | `GET`, `PUT`, `DELETE` | `ADMIN` |
| Payment Methods | `/api/v1/payment-methods` | `GET`, `POST` | `ADMIN` |
| Payment Methods | `/api/v1/payment-methods/{id}` | `GET`, `DELETE` | `ADMIN` |

Notes:
- Endpoints outside this matrix may still allow authenticated user-level access depending on domain behavior.
- Enforcement is implemented at controller level with Spring Security `@PreAuthorize` rules.

## 📊 Monitoring & Logging

- **Distributed Tracing**: Jaeger for request tracing across services
- **Centralized Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Metrics**: Prometheus + Grafana dashboards
- **Health Checks**: Spring Boot Actuator endpoints

## 🧪 Testing

### Unit & Integration Tests

```bash
# Run all unit tests
./mvnw test

# Run with coverage report (output: target/site/jacoco/)
./mvnw test jacoco:report

# Integration tests
./mvnw verify -P integration-tests
```

### Smoke Tests

```bash
# Linux/macOS:
bash scripts/smoke-tests.sh

# Windows:
powershell -ExecutionPolicy Bypass -File scripts\smoke-tests.ps1 -Environment local
```

### Load Tests (k6 required)

```bash
# Linux/macOS — capacity mode (200 OK expected):
bash scripts/run-load-tests.sh --mode capacity --vus 50 --duration 60s

# Windows:
powershell -ExecutionPolicy Bypass -File scripts\run-load-tests.ps1 -Mode capacity -Vus 50 -Duration 60s

# Protection mode (200 + 429 accepted — rate-limiting validation):
powershell -ExecutionPolicy Bypass -File scripts\run-load-tests.ps1 -Mode protection -Vus 200 -Duration 30s
```

Reports are written to `docs/reports/load/`. See [docs/load-testing.md](docs/load-testing.md) for full usage.

### Failover Tests

```bash
# Simulate payment-service outage and measure recovery time (Windows):
powershell -ExecutionPolicy Bypass -File scripts\run-failover-tests.ps1 `
  -WarmupSeconds 30 -OutageSeconds 60 -PostRecoverySeconds 30 -Vus 10
```

Reports are written to `docs/reports/load/`.

## 📝 API Documentation

API documentation is available at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

## 🤝 Contributing

1. Create a feature branch from `develop`
2. Make changes following coding standards
3. Write/update unit tests
4. Create PR with descriptive title
5. Pass CI/CD checks (Jenkins + SonarQube)
6. Get code review approval
7. Merge to `develop`

## 📄 License

MIT License - See [LICENSE](LICENSE) for details.

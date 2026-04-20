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

### Development Setup

```bash
# Clone the repository
git clone <repository-url>
cd travel-plan

# Start infrastructure services
docker-compose -f docker/docker-compose.infra.yml up -d

# Start all microservices
docker-compose -f docker/docker-compose.services.yml up -d

# Start admin dashboard
cd admin-dashboard
npm install
npm run dev
```

### Using Ansible

```bash
# Deploy entire stack
cd ansible
ansible-playbook -i inventory/hosts.yml playbooks/deploy-all.yml

# Deploy specific service
ansible-playbook -i inventory/hosts.yml playbooks/deploy-service.yml -e "service=user-service"
```

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

## 📊 Monitoring & Logging

- **Distributed Tracing**: Jaeger for request tracing across services
- **Centralized Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Metrics**: Prometheus + Grafana dashboards
- **Health Checks**: Spring Boot Actuator endpoints

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report

# Integration tests
./mvnw verify -P integration-tests
```

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

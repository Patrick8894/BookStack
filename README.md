# 📚 BookStack - Library Management System

BookStack is a **production-ready, full-stack Library Management System** for managing books, users, and lending operations with comprehensive monitoring and observability.  
The system features **enterprise-grade role-based access control**, **real-time monitoring**, and **automated alerting** - all containerized for seamless deployment.

---

## ✨ Core Features

### 🔐 Advanced Authentication & Authorization
- **JWT-based authentication** with Spring Security
- **Role-based access control (RBAC)** with method-level security
- **Three-tier user system**:
  - **Admin** – Full system access, user management, system configuration
  - **Librarian** – Book management, borrowing operations, user assistance  
  - **Member** – Book browsing, personal borrow history, self-service returns

### 📖 Comprehensive Book Management
- **GraphQL API** for efficient book queries and filtering
- **Advanced search** by title, author, category
- **Inventory tracking** with real-time availability updates
- **Soft deletion** for data integrity and audit trails
- **Concurrent access protection** for race condition handling

### 🔄 Smart Borrowing System  
- **Automated borrowing workflow** with due date calculations
- **Real-time availability updates** with optimistic locking
- **Overdue detection** with automated status updates
- **Flexible return processing** with notes and timestamps
- **Borrowing history** with comprehensive audit trails

### 🔍 Advanced Search & Discovery
- **GraphQL-powered queries** for optimal performance
- **Responsive UI** with modern Vue 3 + Vuetify design

### 📊 Enterprise Monitoring & Observability
- **EFK Stack** (Elasticsearch + Fluentd + Kibana) for centralized logging
- **Prometheus + Grafana** for metrics collection and visualization  
- **Custom alerting rules** for system health and business metrics
- **Email notifications** for critical system events
- **Performance monitoring** with request tracing and rate limiting

---

## 🛠 Tech Stack

### **Frontend**
- **Nuxt 3** (Vue.js framework) with SSR/SPA modes
- **Vuetify 3** for Material Design UI components  
- **GraphQL** with custom error handling
- **Pinia** for state management
- **TypeScript** for type safety

### **Backend**  
- **Spring Boot 3** with Java 21
- **Spring Security** with JWT authentication
- **Spring GraphQL** for efficient data fetching
- **Spring Data JPA** with PostgreSQL
- **AspectJ** for cross-cutting concerns (auth, logging)
- **Caffeine Cache** for rate limiting and performance

### **Database & Persistence**
- **PostgreSQL** with advanced queries and indexing
- **Spring Data JPA** with custom repositories
- **Connection pooling** and transaction management

### **DevOps & Infrastructure**
- **Docker Compose** for multi-service orchestration
- **Multi-stage Docker builds** for optimized images
- **Environment-specific configurations** (dev/prod)
- **Health checks** and container orchestration

### **Monitoring & Observability**
- **Prometheus** for metrics collection and alerting
- **Grafana** for visualization and dashboards
- **Elasticsearch** for log aggregation and search
- **Fluentd** for log collection and processing  
- **Kibana** for log analysis and visualization

---

## 🚀 Project Status

- ✅ **Backend Complete** - Production-ready Spring Boot API
- ✅ **Frontend Complete** - Modern Nuxt 3 + Vuetify SPA  
- ✅ **Authentication & Authorization** - JWT + RBAC implemented
- ✅ **Database Design** - Optimized PostgreSQL schema
- ✅ **Docker Containerization** - Full multi-service setup
- ✅ **Monitoring Stack** - EFK + Prometheus + Grafana
- ✅ **Automated Testing** - Unit, Integration, and E2E tests
- ✅ **CI/CD Pipeline** - GitHub Actions with automated testing

---

## 🏗 Project Architecture

```
bookstack/
├── 📁 backend/                    # Spring Boot Backend
│   ├── 📂 src/main/java/
│   │   ├── 🔐 auth/               # JWT Authentication & Authorization  
│   │   ├── 📚 book/               # Book Management (GraphQL + REST)
│   │   ├── 👥 user/               # User Management (GraphQL)  
│   │   ├── 🔄 borrow/             # Borrowing System (REST)
│   │   └── ⚙️ common/             # Cross-cutting concerns (security, config)
│   ├── 📂 src/test/java/          # Comprehensive Test Suite
│   │   ├── 🧪 unit/               # Unit Tests
│   │   ├── 🔗 integration/        # Integration Tests  
│   │   └── 🌐 e2e/                # End-to-End Tests
│   ├── 🐳 Dockerfile              # Multi-stage production build
│   └── 📝 pom.xml                 # Maven dependencies
├── 📁 frontend/                   # Nuxt 3 Frontend
│   ├── 📂 pages/                  # Vue 3 + Vuetify pages
│   │   ├── 📚 books/              # Book browsing & management
│   │   ├── 👥 users/              # User management (Admin only)
│   │   ├── 🔄 borrow/             # Borrowing interface
│   │   └── 👤 my-borrow/          # Personal borrow history
│   ├── 📂 components/             # Reusable Vue components
│   ├── 📂 services/               # API integration layer
│   ├── 📂 utils/                  # Validation & error handling
│   ├── 🐳 Dockerfile              # Optimized Node.js build
│   └── ⚙️ nuxt.config.ts          # Nuxt configuration
├── 📁 monitoring/                 # Observability Stack
│   ├── 📊 prometheus/             # Metrics collection & alerting
│   ├── 📈 grafana/                # Dashboards & visualization
│   ├── 🔍 elasticsearch/          # Log storage & indexing
│   ├── 📋 fluentd/                # Log collection & processing
│   └── 👁️ kibana/                 # Log analysis interface
├── 📁 .github/workflows/          # CI/CD Pipeline
│   └── 🔄 ci.yml                  # Automated testing & deployment
├── 🐳 docker-compose.yml          # Production orchestration
├── 🐳 docker-compose.dev.yml      # Development environment
└── 📖 README.md                   # This file
```

---

## 🚀 Quick Start

### **Prerequisites**
- Docker & Docker Compose
- Git

### **Environment Setup (Required)**

Before starting the application, you'll need environment configuration files that contain sensitive data (database credentials, JWT secrets, API keys, etc.).

**🔐 These files are not included in the repository for security reasons.**

#### **📧 Request Required Files**
To get the application running, contact: **patrickwu8894@gmail.com**

**Request the following files:**
- `backend/env/.env.dev` - Development environment variables
- `backend/env/.env.prod` - Production environment variables  
- `backend/env/.env.test` - Test environment variables
- `frontend/.env` - Frontend configuration
- Updated `monitoring/alertmanager/alertmanager.yml` - Email alert configuration

> **⚠️ Note**: Email alerts require manual configuration of private SMTP credentials in the alertmanager configuration.

### **1. Clone & Start**
```bash
git clone <repository-url>
cd bookstack

# Start all services (backend + frontend + database + monitoring)
docker-compose up -d

# Or for development with hot reload
docker-compose -f docker-compose.dev.yml up -d
```

### **2. Access the Application**
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **GraphQL Playground**: http://localhost:8080/graphiql
- **API Documentation**: http://localhost:8080/swagger-ui.html

### **3. Monitoring & Observability**
- **Grafana Dashboards**: http://localhost:3001 (admin/password)
- **Prometheus Metrics**: http://localhost:9090
- **Kibana Logs**: http://localhost:5601
- **Elasticsearch**: http://localhost:9200

### **4. Default Users**
- **Admin**: `admin` / `password`
- **Librarian**: `librarian` / `password`  
- **Member**: `member` / `password`

---

## 🧪 Testing

### **Run Backend Tests**
```bash
cd backend
./script/run-test.sh            # All tests
```

### **Test Coverage**
- **Unit Tests**: Service layer, repositories, controllers
- **Integration Tests**: Full Spring context with test database
- **E2E Tests**: Complete user workflows across multiple services
- **Security Tests**: Authentication, authorization, RBAC
- **Concurrency Tests**: Race conditions and data consistency

---

## 📊 Monitoring & Alerts

### **Automated Alerting**
- **System Health**: CPU, memory, disk usage alerts
- **Application Metrics**: Response times, error rates, throughput
- **Email Notifications**: Critical alerts sent to administrators

### **Pre-configured Dashboards**
- **Application Performance**: Request metrics, response times, error rates
- **Infrastructure Health**: Resource utilization, container status

---

## 🔧 Development

### **Backend Development**
```bash
cd backend
./script/run-dev.sh             # Start with dev profile
./script/run-test.sh            # Run tests
./mvnw clean package            # Build JAR
```

### **Frontend Development**  
```bash
cd frontend
npm install                     # Install dependencies
npm run dev                     # Start dev server with HMR
npm run build                   # Production build
npm run preview                 # Preview production build
```

### **Environment Configuration**
- **Development**: `backend/env/.env.dev` (auto-reload, debug logging)
- **Production**: Environment variables or Docker secrets
- **Testing**: `application-test.properties` (H2 in-memory database)

---

## 🏆 Key Features & Highlights

### **🔒 Security First**
- JWT-based stateless authentication
- Method-level authorization with AspectJ
- Rate limiting and DDoS protection
- SQL injection prevention with parameterized queries
- XSS protection with content security policies

### **⚡ Performance Optimized**
- GraphQL for efficient data fetching
- Database query optimization with JPA
- Caching with Caffeine for rate limiting
- Connection pooling and transaction management
- Optimistic locking for concurrent operations

### **📈 Production Ready**
- Comprehensive monitoring and alerting
- Structured logging with correlation IDs
- Health checks and graceful shutdown
- Multi-environment configuration management

### **🧪 Quality Assured**
- 85%+ test coverage across all layers
- Automated CI/CD pipeline with GitHub Actions
- E2E testing of critical user workflows
- Security testing and vulnerability scanning
- Performance testing and load validation

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Patrick8894/bookstack/issues)
- **Email**: patrickwu8894@gmail.com

---

**Built with ❤️ for modern library management**
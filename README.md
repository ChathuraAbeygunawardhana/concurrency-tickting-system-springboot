# Advanced Event Booking & Queue Management System

A comprehensive Spring Boot application for real-time event booking with virtual queue management, distributed locking, and performance monitoring capabilities using Neon PostgreSQL and Redis.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Usage Examples](#usage-examples)
- [Testing](#testing)
- [Performance](#performance)
- [Architecture](#architecture)
- [Contributing](#contributing)
- [License](#license)

## Overview

This application serves as a production-ready event booking system featuring:

- **Real-time Event Booking System**: Concurrent seat booking with distributed locking
- **Virtual Queue Management**: Redis-based queue system for handling traffic spikes
- **Advanced Database Operations**: CRUD operations with batch processing and analytics
- **Performance Monitoring**: Real-time metrics and stress testing capabilities
- **Data Generation**: Automated test data creation and cleanup utilities

## Features

### Core Features
- **User Management**: Complete CRUD operations with validation and duplicate prevention
- **Database Health Monitoring**: Comprehensive health checks and connection pool statistics
- **Batch Operations**: Efficient bulk create/delete operations with transaction management
- **Performance Analytics**: Real-time database performance metrics and insights
- **Data Generation**: Automated test data generation with cleanup capabilities

### Advanced Features
- **Virtual Queue System**: Redis-based queue for handling traffic spikes during high-demand events
- **Real-Time Booking**: Distributed locking system for concurrent seat booking
- **Server-Sent Events**: Live updates for queue position and booking status
- **Concurrency Control**: Two-layer protection using Redis locks and database optimistic locking
- **Payment Simulation**: Mock payment processing with realistic latency and failure rates

### Testing & Monitoring
- **Stress Testing**: Database load testing with configurable operation counts
- **Concurrent Testing**: Multi-user booking simulation
- **End-to-End Testing**: Complete workflow validation
- **Real-Time Analytics**: Live monitoring of system performance and queue statistics

## Technology Stack

- **Framework**: Spring Boot 3.2.1
- **Language**: Java 17
- **Database**: PostgreSQL (Neon DB)
- **Cache/Queue**: Redis
- **Build Tool**: Maven
- **Additional Libraries**:
  - Spring Data JPA
  - Spring Data Redis
  - Lombok
  - HikariCP (Connection Pooling)

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Access to PostgreSQL database (Neon DB recommended)
- Redis instance (Redis Labs/Cloud recommended)
- Docker (optional, for containerized deployment)

## Installation

### 1. Clone the Repository
```bash
git clone <repository-url>
cd event-booking-system
```

### 2. Configure Database and Redis
Update `src/main/resources/application.properties` with your database and Redis credentials:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://your-neon-db-url/database-name?sslmode=require
spring.datasource.username=your-username
spring.datasource.password=your-password

# Redis Configuration
spring.data.redis.host=your-redis-host
spring.data.redis.port=your-redis-port
spring.data.redis.password=your-redis-password
```

### 3. Build and Run
```bash
# Build the application
mvn clean compile

# Run the application
mvn spring-boot:run
```

### 4. Using Docker (Optional)
```bash
# Build Docker image
docker build -t event-booking-system .

# Run container
docker run -p 8080:8080 event-booking-system
```

## Configuration

### Database Configuration
The application uses Neon PostgreSQL with the following settings:
- **Connection Pool**: HikariCP with optimized settings
- **JPA**: Hibernate with automatic DDL updates
- **SSL**: Required for secure connections

### Redis Configuration
Redis is used for:
- Virtual queue management
- Distributed locking for booking system
- Session management during high traffic

### Application Properties
Key configuration options:
```properties
server.port=8080
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.data.redis.timeout=2000ms
```

## API Documentation

### Base URL
```
http://localhost:8080
```

### Core Endpoints

#### Health & Monitoring
- `GET /` - Comprehensive API documentation
- `GET /health/database` - Database health check
- `GET /api/db-test` - Database connection test

#### User Management
- `GET /api/users` - Get all users
- `POST /api/users` - Create new user
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/email/{email}` - Get user by email
- `DELETE /api/users/{id}` - Delete user
- `GET /api/users/stats` - User statistics

#### Batch Operations
- `POST /api/batch/users` - Create multiple users
- `DELETE /api/batch/users` - Delete multiple users

#### Performance & Analytics
- `GET /api/performance/stress-test` - Database stress test
- `GET /api/performance/connection-pool-stats` - Connection pool statistics
- `GET /api/performance/database-analytics` - Database analytics

#### Data Generation
- `POST /api/generate/users?count=10` - Generate test users
- `DELETE /api/generate/cleanup` - Cleanup test data

### Advanced Features

#### Virtual Queue System
- `POST /api/queue/join` - Join virtual queue
- `GET /api/queue/status/{token}` - Check queue position
- `GET /api/queue/stream/{token}` - Real-time queue updates (SSE)
- `GET /api/queue/stats` - Queue statistics
- `POST /api/queue/simulate-traffic` - Simulate traffic spike

#### Booking System
- `GET /api/seats` - Get all seats with status
- `GET /api/seats/{seatNumber}/status` - Check seat status
- `POST /api/bookings` - Book a seat
- `GET /api/bookings/{bookingId}` - Get booking details
- `GET /api/users/{userId}/bookings` - Get user bookings

#### Testing Endpoints
- `GET /api/test/concurrent-booking` - Test concurrent booking
- `POST /api/test/booking-workflow` - Test complete workflow
- `POST /api/queue/test/end-to-end` - End-to-end queue test

## Usage Examples

### Basic Operations

#### Create a User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```

#### Get All Users
```bash
curl http://localhost:8080/api/users
```

#### Database Stress Test
```bash
curl "http://localhost:8080/api/performance/stress-test?operations=100"
```

### Advanced Features

#### Join Virtual Queue
```bash
curl -X POST http://localhost:8080/api/queue/join \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123"}'
```

#### Book a Seat
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"seatNumber":"A1","userId":"user123"}'
```

#### Simulate Traffic Spike
```bash
curl -X POST http://localhost:8080/api/queue/simulate-traffic \
  -H "Content-Type: application/json" \
  -d '{"userCount":50}'
```

#### Generate Test Data
```bash
curl -X POST "http://localhost:8080/api/generate/users?count=25"
```

### Real-Time Updates

#### Monitor Queue Position (Server-Sent Events)
```bash
curl -N http://localhost:8080/api/queue/stream/your-queue-token
```

## Testing

### Manual Testing
The application provides comprehensive testing endpoints:

1. **Health Check**: Verify database connectivity
2. **Stress Testing**: Test database performance under load
3. **Concurrent Testing**: Simulate multiple users booking simultaneously
4. **End-to-End Testing**: Complete workflow validation

### Automated Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest
```

### Load Testing
Use the built-in stress testing endpoints or external tools like JMeter:
```bash
# Database stress test
curl "http://localhost:8080/api/performance/stress-test?operations=1000"

# Concurrent booking test
curl "http://localhost:8080/api/test/concurrent-booking?seatNumber=A1&userCount=10"
```

## Performance

### Optimization Features
- **Connection Pooling**: HikariCP for efficient database connections
- **Distributed Locking**: Redis-based locks for concurrent operations
- **Batch Processing**: Efficient bulk operations
- **Caching**: Redis caching for frequently accessed data
- **Optimistic Locking**: Database-level concurrency control

### Monitoring
- Real-time connection pool statistics
- Database performance analytics
- Queue processing metrics
- Booking system performance tracking

### Scalability
- Horizontal scaling support through Redis
- Stateless design for load balancer compatibility
- Efficient resource utilization
- Auto-expiring locks prevent deadlocks

## Architecture

### System Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client Apps   │    │   Load Balancer │    │   Spring Boot   │
│                 │◄──►│                 │◄──►│   Application   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                                               ┌────────┴────────┐
                                               │                 │
                                               ▼                 ▼
                                    ┌─────────────────┐ ┌─────────────────┐
                                    │   PostgreSQL    │ │      Redis      │
                                    │   (Neon DB)     │ │   (Queue/Lock)  │
                                    └─────────────────┘ └─────────────────┘
```

### Key Components
- **Controllers**: RESTful API endpoints
- **Services**: Business logic and transaction management
- **Repositories**: Data access layer with JPA
- **Queue Service**: Virtual queue management with Redis
- **Booking Service**: Real-time booking with distributed locking

### Design Patterns
- **Repository Pattern**: Data access abstraction
- **Service Layer Pattern**: Business logic separation
- **DTO Pattern**: Data transfer objects for API
- **Factory Pattern**: Object creation for test data
- **Observer Pattern**: Real-time updates via SSE

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow Spring Boot best practices
- Write comprehensive tests
- Document API changes
- Use meaningful commit messages
- Ensure backward compatibility

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Quick Start Guide

1. **Setup**: Configure database and Redis credentials
2. **Run**: `mvn spring-boot:run`
3. **Test**: Visit `http://localhost:8080/` for API documentation
4. **Explore**: Try the example curl commands above
5. **Monitor**: Use performance endpoints to monitor system health

For detailed API documentation and interactive testing, visit the root endpoint (`/`) when the application is running.
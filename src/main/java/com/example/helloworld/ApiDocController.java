package com.example.helloworld;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ApiDocController {

    @GetMapping("/")
    public Map<String, Object> getApiDocumentation() {
        Map<String, Object> doc = new HashMap<>();
        
        doc.put("title", "Advanced Event Booking & Queue Management API");
        doc.put("description", "A comprehensive Spring Boot application for real-time event booking with virtual queue management and performance monitoring");
        doc.put("status", "Connected to Neon DB with full functionality");
        doc.put("version", "2.0.0");
        
        Map<String, Object> endpoints = new HashMap<>();
        
        // Basic endpoints
        endpoints.put("GET /", "This comprehensive API documentation");
        
        // Database health & monitoring
        endpoints.put("GET /health/database", "Comprehensive database health check");
        endpoints.put("GET /test-db-connection", "Simple database connection test");
        endpoints.put("GET /api/db-test", "Database connection test with user count");
        
        // User management (CRUD)
        endpoints.put("GET /api/users", "Get all users");
        endpoints.put("POST /api/users", "Create a new user (JSON: {name, email})");
        endpoints.put("GET /api/users/{id}", "Get user by ID");
        endpoints.put("GET /api/users/email/{email}", "Get user by email");
        endpoints.put("DELETE /api/users/{id}", "Delete user by ID");
        endpoints.put("GET /api/users/stats", "Get user statistics");
        
        // Batch operations
        endpoints.put("POST /api/batch/users", "Create multiple users in batch (JSON array)");
        endpoints.put("DELETE /api/batch/users", "Delete multiple users by IDs (JSON array)");
        
        // Performance & monitoring
        endpoints.put("GET /api/performance/stress-test", "Database stress test (?operations=100)");
        endpoints.put("GET /api/performance/connection-pool-stats", "Connection pool statistics");
        endpoints.put("GET /api/performance/database-analytics", "Comprehensive database analytics");
        
        // Data generation & testing
        endpoints.put("POST /api/generate/users", "Generate test users (?count=10)");
        endpoints.put("DELETE /api/generate/cleanup", "Cleanup generated test data");
        endpoints.put("GET /api/generate/sample-data", "Sample data generation info");
        
        // Virtual Queue System (Traffic Spike Management)
        endpoints.put("POST /api/queue/join", "Join virtual queue during high traffic");
        endpoints.put("GET /api/queue/status/{token}", "Check position in queue");
        endpoints.put("GET /api/queue/stream/{token}", "Real-time queue updates (SSE)");
        endpoints.put("GET /api/queue/stats", "Queue statistics (admin)");
        endpoints.put("POST /api/queue/process", "Process queue manually (admin)");
        endpoints.put("POST /api/queue/simulate-traffic", "Simulate traffic spike");
        endpoints.put("DELETE /api/queue/active/{userId}", "Remove user from active session");
        
        // Virtual Queue Testing
        endpoints.put("POST /api/queue/test/end-to-end", "Complete queue->booking workflow test");
        endpoints.put("POST /api/queue/test/concurrent-join", "Test concurrent queue joining");
        endpoints.put("POST /api/queue/test/load-test", "Queue load testing with processing");
        endpoints.put("DELETE /api/queue/test/reset", "Reset queue for testing");
        
        // Booking System (Real-Time Event Ticket Booking)
        endpoints.put("GET /api/seats", "Get all seats with status");
        endpoints.put("GET /api/seats/{seatNumber}/status", "Check specific seat status (e.g., /api/seats/A1/status)");
        endpoints.put("POST /api/bookings", "Book a seat (JSON: {seatNumber, userId})");
        endpoints.put("GET /api/bookings/{bookingId}", "Get booking details by ID");
        endpoints.put("GET /api/users/{userId}/bookings", "Get user's bookings");
        
        // Booking System Testing
        endpoints.put("GET /api/test/health", "Booking system health check");
        endpoints.put("GET /api/test/concurrent-booking", "Test concurrent booking (?seatNumber=A1&userCount=5)");
        endpoints.put("POST /api/test/booking-workflow", "Test complete booking workflow");
        
        doc.put("endpoints", endpoints);
        
        Map<String, Object> examples = new HashMap<>();
        examples.put("Stress Test", "curl 'http://localhost:8080/api/performance/stress-test?operations=50'");
        examples.put("Batch Create", "curl -X POST http://localhost:8080/api/batch/users -H 'Content-Type: application/json' -d '[{\"name\":\"User1\",\"email\":\"user1@test.com\"},{\"name\":\"User2\",\"email\":\"user2@test.com\"}]'");
        examples.put("Generate Data", "curl -X POST 'http://localhost:8080/api/generate/users?count=25'");
        examples.put("Analytics", "curl http://localhost:8080/api/performance/database-analytics");
        examples.put("Health Check", "curl http://localhost:8080/health/database");
        examples.put("Get Users", "curl http://localhost:8080/api/users");
        
        examples.put("Join Queue", "curl -X POST http://localhost:8080/api/queue/join -H 'Content-Type: application/json' -d '{\"userId\":\"user123\"}'");
        examples.put("Check Queue", "curl http://localhost:8080/api/queue/status/queue_abc123def456");
        examples.put("Traffic Spike", "curl -X POST http://localhost:8080/api/queue/simulate-traffic -H 'Content-Type: application/json' -d '{\"userCount\":50}'");
        examples.put("Queue Stats", "curl http://localhost:8080/api/queue/stats");
        examples.put("End-to-End Test", "curl -X POST http://localhost:8080/api/queue/test/end-to-end -H 'Content-Type: application/json' -d '{\"userId\":\"testUser\",\"seatNumber\":\"A1\"}'");
        examples.put("Concurrent Queue", "curl -X POST http://localhost:8080/api/queue/test/concurrent-join -H 'Content-Type: application/json' -d '{\"userCount\":10}'");
        
        examples.put("Book Seat", "curl -X POST http://localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{\"seatNumber\":\"A1\",\"userId\":\"alice@neondb.com\"}'");
        examples.put("Check Seat", "curl http://localhost:8080/api/seats/A1/status");
        examples.put("Concurrent Test", "curl 'http://localhost:8080/api/test/concurrent-booking?seatNumber=A2&userCount=3'");
        examples.put("List Seats", "curl http://localhost:8080/api/seats");
        examples.put("Booking Workflow", "curl -X POST http://localhost:8080/api/test/booking-workflow -H 'Content-Type: application/json' -d '{\"seatNumber\":\"A3\",\"userId\":\"testUser\"}'");
        
        doc.put("examples", examples);
        
        Map<String, Object> features = new HashMap<>();
        features.put("Transaction Management", "All batch operations use database transactions");
        features.put("Performance Monitoring", "Real-time database performance metrics");
        features.put("Data Generation", "Automated test data generation with cleanup");
        features.put("Stress Testing", "Database load testing capabilities");
        features.put("Analytics", "Comprehensive database analytics and insights");
        features.put("Connection Pooling", "HikariCP connection pool monitoring");
        features.put("Validation", "Input validation and duplicate prevention");
        features.put("Batch Operations", "Efficient bulk create/delete operations");
        
        features.put("Virtual Queue System", "Redis-based queue for handling traffic spikes (X-Factor feature)");
        features.put("Real-Time Updates", "Server-Sent Events for live queue position updates");
        features.put("Auto Queue Processing", "Scheduled processing to admit users from queue");
        features.put("Smart Admission", "Direct admission when capacity available, queue when overloaded");
        features.put("Queue Analytics", "Real-time monitoring of queue size and processing rates");
        
        features.put("Real-Time Booking", "Distributed locking with Redis for concurrent seat booking");
        features.put("Concurrency Control", "Two-layer protection: Redis locks + Database optimistic locking");
        features.put("Payment Simulation", "Mock payment with 2s latency and 10% failure rate");
        features.put("Lock Management", "Auto-expiring locks (5min TTL) prevent deadlocks");
        features.put("Fail-Fast Design", "Immediate rejection of concurrent booking attempts");
        
        doc.put("features", features);
        
        return doc;
    }
}
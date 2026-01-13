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
        
        doc.put("title", "🚀 Advanced Neon DB Test API");
        doc.put("description", "A comprehensive Spring Boot application demonstrating advanced Neon PostgreSQL database operations");
        doc.put("status", "✅ Connected to Neon DB with full functionality");
        doc.put("version", "2.0.0");
        
        Map<String, Object> endpoints = new HashMap<>();
        
        // Basic endpoints
        endpoints.put("GET /", "📖 This comprehensive API documentation");
        endpoints.put("GET /api/hello", "👋 Simple hello world message");
        
        // Database health & monitoring
        endpoints.put("GET /health/database", "🏥 Comprehensive database health check");
        endpoints.put("GET /test-db-connection", "🔌 Simple database connection test");
        endpoints.put("GET /api/db-test", "📊 Database connection test with user count");
        
        // User management (CRUD)
        endpoints.put("GET /api/users", "👥 Get all users");
        endpoints.put("POST /api/users", "➕ Create a new user (JSON: {name, email})");
        endpoints.put("GET /api/users/{id}", "👤 Get user by ID");
        endpoints.put("GET /api/users/email/{email}", "📧 Get user by email");
        endpoints.put("DELETE /api/users/{id}", "🗑️ Delete user by ID");
        endpoints.put("GET /api/users/stats", "📈 Get user statistics");
        
        // Batch operations
        endpoints.put("POST /api/batch/users", "⚡ Create multiple users in batch (JSON array)");
        endpoints.put("DELETE /api/batch/users", "🗑️ Delete multiple users by IDs (JSON array)");
        
        // Performance & monitoring
        endpoints.put("GET /api/performance/stress-test", "🔥 Database stress test (?operations=100)");
        endpoints.put("GET /api/performance/connection-pool-stats", "🏊 Connection pool statistics");
        endpoints.put("GET /api/performance/database-analytics", "📊 Comprehensive database analytics");
        
        // Data generation & testing
        endpoints.put("POST /api/generate/users", "🎲 Generate test users (?count=10)");
        endpoints.put("DELETE /api/generate/cleanup", "🧹 Cleanup generated test data");
        endpoints.put("GET /api/generate/sample-data", "📋 Sample data generation info");
        
        doc.put("endpoints", endpoints);
        
        Map<String, Object> examples = new HashMap<>();
        examples.put("🔥 Stress Test", "curl 'http://localhost:8080/api/performance/stress-test?operations=50'");
        examples.put("⚡ Batch Create", "curl -X POST http://localhost:8080/api/batch/users -H 'Content-Type: application/json' -d '[{\"name\":\"User1\",\"email\":\"user1@test.com\"},{\"name\":\"User2\",\"email\":\"user2@test.com\"}]'");
        examples.put("🎲 Generate Data", "curl -X POST 'http://localhost:8080/api/generate/users?count=25'");
        examples.put("📊 Analytics", "curl http://localhost:8080/api/performance/database-analytics");
        examples.put("🏥 Health Check", "curl http://localhost:8080/health/database");
        examples.put("👥 Get Users", "curl http://localhost:8080/api/users");
        
        doc.put("examples", examples);
        
        Map<String, Object> features = new HashMap<>();
        features.put("🔄 Transaction Management", "All batch operations use database transactions");
        features.put("⚡ Performance Monitoring", "Real-time database performance metrics");
        features.put("🎲 Data Generation", "Automated test data generation with cleanup");
        features.put("🔥 Stress Testing", "Database load testing capabilities");
        features.put("📊 Analytics", "Comprehensive database analytics and insights");
        features.put("🏊 Connection Pooling", "HikariCP connection pool monitoring");
        features.put("✅ Validation", "Input validation and duplicate prevention");
        features.put("🗑️ Batch Operations", "Efficient bulk create/delete operations");
        
        doc.put("features", features);
        
        return doc;
    }
}
package com.example.helloworld;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private UserService userService;

    @GetMapping("/stress-test")
    public Map<String, Object> performStressTest(@RequestParam(defaultValue = "100") int operations) {
        Map<String, Object> result = new HashMap<>();
        List<Long> operationTimes = new ArrayList<>();
        
        long totalStartTime = System.currentTimeMillis();
        
        try {
            // Test database read performance
            for (int i = 0; i < operations; i++) {
                long startTime = System.nanoTime();
                userService.getUserCount();
                long endTime = System.nanoTime();
                operationTimes.add((endTime - startTime) / 1_000_000); // Convert to milliseconds
            }
            
            long totalEndTime = System.currentTimeMillis();
            
            // Calculate statistics
            long totalTime = totalEndTime - totalStartTime;
            double avgTime = operationTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            long minTime = operationTimes.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxTime = operationTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            double throughput = (double) operations / (totalTime / 1000.0); // operations per second
            
            result.put("success", true);
            result.put("operations", operations);
            result.put("totalTimeMs", totalTime);
            result.put("averageTimeMs", avgTime);
            result.put("minTimeMs", minTime);
            result.put("maxTimeMs", maxTime);
            result.put("throughputOpsPerSec", throughput);
            result.put("message", "Stress test completed successfully");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Stress test failed: " + e.getMessage());
        }
        
        return result;
    }
    
    @GetMapping("/connection-pool-stats")
    public Map<String, Object> getConnectionPoolStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            stats.put("connectionValid", connection.isValid(5));
            stats.put("autoCommit", connection.getAutoCommit());
            stats.put("readOnly", connection.isReadOnly());
            stats.put("transactionIsolation", connection.getTransactionIsolation());
            stats.put("dataSourceClass", dataSource.getClass().getSimpleName());
            
            // Try to get HikariCP specific stats if available
            if (dataSource.getClass().getSimpleName().equals("HikariDataSource")) {
                try {
                    com.zaxxer.hikari.HikariDataSource hikariDS = (com.zaxxer.hikari.HikariDataSource) dataSource;
                    com.zaxxer.hikari.HikariPoolMXBean poolBean = hikariDS.getHikariPoolMXBean();
                    
                    stats.put("activeConnections", poolBean.getActiveConnections());
                    stats.put("idleConnections", poolBean.getIdleConnections());
                    stats.put("totalConnections", poolBean.getTotalConnections());
                    stats.put("threadsAwaitingConnection", poolBean.getThreadsAwaitingConnection());
                } catch (Exception e) {
                    stats.put("hikariStatsError", "Could not retrieve HikariCP stats: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            stats.put("error", "Failed to get connection pool stats: " + e.getMessage());
        }
        
        return stats;
    }
    
    @GetMapping("/database-analytics")
    public Map<String, Object> getDatabaseAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            // Get table size information
            String tableSizeQuery = "SELECT pg_size_pretty(pg_total_relation_size('users')) as table_size, " +
                                  "pg_size_pretty(pg_relation_size('users')) as data_size, " +
                                  "pg_size_pretty(pg_total_relation_size('users') - pg_relation_size('users')) as index_size";
            
            try (PreparedStatement stmt = connection.prepareStatement(tableSizeQuery);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    analytics.put("tableSize", rs.getString("table_size"));
                    analytics.put("dataSize", rs.getString("data_size"));
                    analytics.put("indexSize", rs.getString("index_size"));
                }
            }
            
            // Get user statistics
            analytics.put("totalUsers", userService.getUserCount());
            
            // Get database version and info
            String versionQuery = "SELECT version(), current_database(), current_user, inet_server_addr(), inet_server_port()";
            try (PreparedStatement stmt = connection.prepareStatement(versionQuery);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    analytics.put("postgresVersion", rs.getString(1));
                    analytics.put("currentDatabase", rs.getString(2));
                    analytics.put("currentUser", rs.getString(3));
                    analytics.put("serverAddress", rs.getString(4));
                    analytics.put("serverPort", rs.getInt(5));
                }
            }
            
            // Test query performance
            long startTime = System.nanoTime();
            userService.getAllUsers();
            long endTime = System.nanoTime();
            analytics.put("getAllUsersQueryTimeMs", (endTime - startTime) / 1_000_000.0);
            
        } catch (Exception e) {
            analytics.put("error", "Failed to get database analytics: " + e.getMessage());
        }
        
        return analytics;
    }
}
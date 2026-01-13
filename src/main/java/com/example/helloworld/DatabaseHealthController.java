package com.example.helloworld;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DatabaseHealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserService userService;

    @GetMapping("/health/database")
    public Map<String, Object> getDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            // Basic connection test
            health.put("status", "UP");
            health.put("connectionValid", connection.isValid(5));
            
            // Database metadata
            DatabaseMetaData metaData = connection.getMetaData();
            health.put("databaseProductName", metaData.getDatabaseProductName());
            health.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            health.put("driverName", metaData.getDriverName());
            health.put("url", metaData.getURL());
            
            // Check if our table exists
            ResultSet tables = metaData.getTables(null, null, "users", null);
            health.put("usersTableExists", tables.next());
            
            // Get current user count
            long userCount = userService.getUserCount();
            health.put("totalUsers", userCount);
            
            // Connection pool info
            health.put("connectionPoolClass", dataSource.getClass().getSimpleName());
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}
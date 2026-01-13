package com.example.helloworld;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@RestController
public class DatabaseTestController {

    @GetMapping("/test-db-connection")
    public String testDatabaseConnection() {
        String url = "jdbc:postgresql://ep-soft-voice-ah80us33-pooler.c-3.us-east-1.aws.neon.tech/neondb?sslmode=require";
        String username = "neondb_owner";
        String password = "npg_S2YT6aFvKoJc";
        
        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            if (connection != null && !connection.isClosed()) {
                connection.close();
                return "Database connection successful! Neon DB is working properly.";
            } else {
                return "Database connection failed - connection is null or closed.";
            }
        } catch (SQLException e) {
            return "Database connection failed: " + e.getMessage();
        }
    }
}
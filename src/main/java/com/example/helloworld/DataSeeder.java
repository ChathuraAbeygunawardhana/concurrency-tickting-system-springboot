package com.example.helloworld;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) throws Exception {
        // Only seed data if no users exist
        if (userService.getUserCount() == 0) {
            System.out.println("🌱 Seeding initial data to Neon DB...");
            
            // Create sample users
            User user1 = new User("Alice Johnson", "alice@neondb.com");
            User user2 = new User("Bob Wilson", "bob@neondb.com");
            User user3 = new User("Carol Davis", "carol@neondb.com");
            
            userService.saveUser(user1);
            userService.saveUser(user2);
            userService.saveUser(user3);
            
            System.out.println("✅ Successfully seeded " + userService.getUserCount() + " users to Neon DB!");
        } else {
            System.out.println("📊 Database already contains " + userService.getUserCount() + " users. Skipping seeding.");
        }
    }
}
package com.example.helloworld;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserService userService;
    
    @Autowired
    private SeatRepository seatRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only seed data if no users exist
        if (userService.getUserCount() == 0) {
            System.out.println("Seeding initial data to Neon DB...");
            
            // Create sample users
            User user1 = new User("Alice Johnson", "alice@neondb.com");
            User user2 = new User("Bob Wilson", "bob@neondb.com");
            User user3 = new User("Carol Davis", "carol@neondb.com");
            
            userService.saveUser(user1);
            userService.saveUser(user2);
            userService.saveUser(user3);
            
            System.out.println("Successfully seeded " + userService.getUserCount() + " users to Neon DB!");
        } else {
            System.out.println("Database already contains " + userService.getUserCount() + " users. Skipping seeding.");
        }
        
        // Seed seats if none exist
        if (seatRepository.count() == 0) {
            System.out.println("Seeding initial seats...");
            
            // Create seats A1-A10, B1-B10, C1-C10 (30 seats total)
            String[] rows = {"A", "B", "C"};
            for (String row : rows) {
                for (int i = 1; i <= 10; i++) {
                    Seat seat = new Seat();
                    seat.setSeatNumber(row + i);
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seatRepository.save(seat);
                }
            }
            
            System.out.println("Successfully seeded " + seatRepository.count() + " seats!");
        } else {
            System.out.println("Database already contains " + seatRepository.count() + " seats. Skipping seeding.");
        }
    }
}
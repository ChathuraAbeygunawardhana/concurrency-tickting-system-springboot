package com.example.helloworld;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/generate")
public class DataGeneratorController {

    @Autowired
    private UserService userService;

    private final String[] firstNames = {
        "Alice", "Bob", "Carol", "David", "Emma", "Frank", "Grace", "Henry", 
        "Ivy", "Jack", "Kate", "Liam", "Mia", "Noah", "Olivia", "Paul",
        "Quinn", "Ruby", "Sam", "Tina", "Uma", "Victor", "Wendy", "Xander", "Yara", "Zoe"
    };

    private final String[] lastNames = {
        "Anderson", "Brown", "Clark", "Davis", "Evans", "Fisher", "Garcia", "Harris",
        "Johnson", "King", "Lee", "Miller", "Nelson", "O'Connor", "Parker", "Quinn",
        "Rodriguez", "Smith", "Taylor", "Underwood", "Valdez", "Wilson", "Xavier", "Young", "Zhang"
    };

    private final String[] domains = {
        "neondb.com", "testmail.com", "example.org", "demo.net", "sample.io", 
        "mockdata.co", "fakemail.dev", "testuser.app"
    };

    @PostMapping("/users")
    @Transactional
    public ResponseEntity<?> generateUsers(@RequestParam(defaultValue = "10") int count) {
        Map<String, Object> result = new HashMap<>();
        List<User> generatedUsers = new ArrayList<>();
        Set<String> usedEmails = new HashSet<>();
        
        // Get existing emails to avoid duplicates
        try {
            List<User> existingUsers = userService.getAllUsers();
            for (User user : existingUsers) {
                usedEmails.add(user.getEmail().toLowerCase());
            }
        } catch (Exception e) {
            result.put("error", "Failed to check existing users: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        long startTime = System.currentTimeMillis();
        Random random = new Random();
        
        try {
            for (int i = 0; i < count; i++) {
                String firstName = firstNames[random.nextInt(firstNames.length)];
                String lastName = lastNames[random.nextInt(lastNames.length)];
                String domain = domains[random.nextInt(domains.length)];
                
                // Generate unique email
                String email;
                int attempts = 0;
                do {
                    String emailPrefix = firstName.toLowerCase() + "." + lastName.toLowerCase();
                    if (attempts > 0) {
                        emailPrefix += attempts;
                    }
                    email = emailPrefix + "@" + domain;
                    attempts++;
                } while (usedEmails.contains(email) && attempts < 100);
                
                if (attempts >= 100) {
                    result.put("warning", "Could not generate unique email after 100 attempts for user " + (i + 1));
                    continue;
                }
                
                usedEmails.add(email);
                
                User user = new User(firstName + " " + lastName, email);
                User savedUser = userService.saveUser(user);
                generatedUsers.add(savedUser);
            }
            
            long endTime = System.currentTimeMillis();
            
            result.put("success", true);
            result.put("requested", count);
            result.put("generated", generatedUsers.size());
            result.put("processingTimeMs", endTime - startTime);
            result.put("averageTimePerUserMs", (double)(endTime - startTime) / generatedUsers.size());
            result.put("generatedUsers", generatedUsers);
            result.put("totalUsersInDatabase", userService.getUserCount());
            result.put("message", "Successfully generated " + generatedUsers.size() + " users");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Failed to generate users: " + e.getMessage());
            result.put("partiallyGenerated", generatedUsers.size());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    @DeleteMapping("/cleanup")
    @Transactional
    public ResponseEntity<?> cleanupGeneratedData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            List<User> allUsers = userService.getAllUsers();
            List<Long> deletedIds = new ArrayList<>();
            
            // Delete users with generated-looking emails (containing domains from our list)
            for (User user : allUsers) {
                for (String domain : domains) {
                    if (user.getEmail().endsWith("@" + domain)) {
                        userService.deleteUser(user.getId());
                        deletedIds.add(user.getId());
                        break;
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            result.put("success", true);
            result.put("deletedCount", deletedIds.size());
            result.put("deletedIds", deletedIds);
            result.put("processingTimeMs", endTime - startTime);
            result.put("remainingUsers", userService.getUserCount());
            result.put("message", "Cleanup completed. Deleted " + deletedIds.size() + " generated users.");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Cleanup failed: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    @GetMapping("/sample-data")
    public ResponseEntity<?> getSampleDataInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("availableFirstNames", firstNames.length);
        info.put("availableLastNames", lastNames.length);
        info.put("availableDomains", domains.length);
        info.put("maxUniqueCombinations", firstNames.length * lastNames.length * domains.length);
        info.put("firstNames", Arrays.asList(firstNames));
        info.put("lastNames", Arrays.asList(lastNames));
        info.put("domains", Arrays.asList(domains));
        info.put("currentUsersInDatabase", userService.getUserCount());
        
        return ResponseEntity.ok(info);
    }
}
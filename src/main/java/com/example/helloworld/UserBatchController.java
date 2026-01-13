package com.example.helloworld;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class UserBatchController {

    @Autowired
    private UserService userService;

    @PostMapping("/users")
    @Transactional
    public ResponseEntity<?> createUsersInBatch(@RequestBody List<User> users) {
        Map<String, Object> result = new HashMap<>();
        List<User> createdUsers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        try {
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                try {
                    // Validate user
                    if (user.getName() == null || user.getName().trim().isEmpty()) {
                        errors.add("User " + (i+1) + ": Name is required");
                        continue;
                    }
                    if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                        errors.add("User " + (i+1) + ": Email is required");
                        continue;
                    }
                    
                    // Check for duplicate email
                    if (userService.getUserByEmail(user.getEmail()).isPresent()) {
                        errors.add("User " + (i+1) + ": Email " + user.getEmail() + " already exists");
                        continue;
                    }
                    
                    User savedUser = userService.saveUser(user);
                    createdUsers.add(savedUser);
                    
                } catch (Exception e) {
                    errors.add("User " + (i+1) + ": " + e.getMessage());
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            result.put("success", true);
            result.put("totalRequested", users.size());
            result.put("successfullyCreated", createdUsers.size());
            result.put("errors", errors);
            result.put("createdUsers", createdUsers);
            result.put("processingTimeMs", endTime - startTime);
            result.put("message", "Batch operation completed. Created " + createdUsers.size() + " out of " + users.size() + " users.");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Batch operation failed: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    @DeleteMapping("/users")
    @Transactional
    public ResponseEntity<?> deleteUsersInBatch(@RequestBody List<Long> userIds) {
        Map<String, Object> result = new HashMap<>();
        List<Long> deletedIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        try {
            for (Long id : userIds) {
                try {
                    if (userService.getUserById(id).isPresent()) {
                        userService.deleteUser(id);
                        deletedIds.add(id);
                    } else {
                        errors.add("User with ID " + id + " not found");
                    }
                } catch (Exception e) {
                    errors.add("Failed to delete user " + id + ": " + e.getMessage());
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            result.put("success", true);
            result.put("totalRequested", userIds.size());
            result.put("successfullyDeleted", deletedIds.size());
            result.put("deletedIds", deletedIds);
            result.put("errors", errors);
            result.put("processingTimeMs", endTime - startTime);
            result.put("message", "Batch deletion completed. Deleted " + deletedIds.size() + " out of " + userIds.size() + " users.");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Batch deletion failed: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
package com.example.helloworld;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/queue/test")
@RequiredArgsConstructor
@Slf4j
public class VirtualQueueTestController {
    
    private final VirtualQueueService queueService;
    private final BookingService bookingService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(20);
    
    /**
     * Complete end-to-end test: Queue -> Booking workflow
     * POST /api/queue/test/end-to-end
     * Body: {"userId": "testUser", "seatNumber": "A1"}
     */
    @PostMapping("/end-to-end")
    public ResponseEntity<Map<String, Object>> testEndToEndWorkflow(@RequestBody EndToEndTestRequest request) {
        log.info("Starting end-to-end test for user: {} and seat: {}", request.getUserId(), request.getSeatNumber());
        
        List<Map<String, Object>> steps = new ArrayList<>();
        
        // Step 1: Join queue
        VirtualQueueService.QueueResult queueResult = queueService.joinQueue(request.getUserId());
        steps.add(Map.of(
            "step", 1,
            "description", "Join virtual queue",
            "result", Map.of(
                "success", queueResult.isSuccess(),
                "status", queueResult.getStatus(),
                "position", queueResult.getPosition() != null ? queueResult.getPosition() : 0,
                "token", queueResult.getToken() != null ? queueResult.getToken() : "",
                "message", queueResult.getMessage()
            )
        ));
        
        // Step 2: If queued, simulate processing
        if ("QUEUED".equals(queueResult.getStatus())) {
            // Process queue to admit the user
            int processed = queueService.processQueue(1);
            steps.add(Map.of(
                "step", 2,
                "description", "Process queue to admit user",
                "result", Map.of(
                    "processed", processed,
                    "message", processed > 0 ? "User processed from queue" : "No users processed"
                )
            ));
        }
        
        // Step 3: Check final queue status
        VirtualQueueService.QueueStatus finalStatus = queueService.checkQueueStatus(queueResult.getToken());
        steps.add(Map.of(
            "step", 3,
            "description", "Check final queue status",
            "result", Map.of(
                "status", finalStatus.getStatus(),
                "message", finalStatus.getMessage()
            )
        ));
        
        // Step 4: Attempt booking if active
        BookingService.BookingResult bookingResult = null;
        if ("ACTIVE".equals(finalStatus.getStatus()) || "ALREADY_ACTIVE".equals(queueResult.getStatus())) {
            bookingResult = bookingService.bookSeat(request.getSeatNumber(), request.getUserId());
            steps.add(Map.of(
                "step", 4,
                "description", "Attempt seat booking",
                "result", Map.of(
                    "success", bookingResult.isSuccess(),
                    "message", bookingResult.getMessage(),
                    "bookingId", bookingResult.getBookingId() != null ? bookingResult.getBookingId() : ""
                )
            ));
        } else {
            steps.add(Map.of(
                "step", 4,
                "description", "Booking skipped - user not active",
                "result", Map.of(
                    "skipped", true,
                    "reason", "User not in active session"
                )
            ));
        }
        
        Map<String, Object> response = Map.of(
            "userId", request.getUserId(),
            "seatNumber", request.getSeatNumber(),
            "overallSuccess", bookingResult != null && bookingResult.isSuccess(),
            "steps", steps,
            "summary", bookingResult != null && bookingResult.isSuccess() ? 
                "End-to-end test completed successfully" : 
                "Test completed but booking was not successful"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test concurrent queue joining
     * POST /api/queue/test/concurrent-join
     * Body: {"userCount": 10}
     */
    @PostMapping("/concurrent-join")
    public ResponseEntity<Map<String, Object>> testConcurrentQueueJoin(@RequestBody ConcurrentTestRequest request) {
        log.info("Testing concurrent queue joining with {} users", request.getUserCount());
        
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
        
        // Create multiple concurrent queue join attempts
        for (int i = 1; i <= request.getUserCount(); i++) {
            String userId = "concurrent_user_" + i;
            CompletableFuture<Map<String, Object>> future = CompletableFuture
                .supplyAsync(() -> {
                    VirtualQueueService.QueueResult result = queueService.joinQueue(userId);
                    return Map.<String, Object>of(
                        "userId", userId,
                        "success", result.isSuccess(),
                        "status", result.getStatus(),
                        "position", result.getPosition() != null ? result.getPosition() : 0,
                        "token", result.getToken() != null ? result.getToken() : "",
                        "message", result.getMessage()
                    );
                }, executorService);
            futures.add(future);
        }
        
        // Wait for all attempts to complete
        List<Map<String, Object>> results = new ArrayList<>();
        int activeCount = 0;
        int queuedCount = 0;
        
        for (CompletableFuture<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> result = future.get();
                results.add(result);
                
                String status = (String) result.get("status");
                if ("ACTIVE".equals(status) || "ALREADY_ACTIVE".equals(status)) {
                    activeCount++;
                } else if ("QUEUED".equals(status)) {
                    queuedCount++;
                }
                
            } catch (Exception e) {
                log.error("Error in concurrent queue join test", e);
                results.add(Map.of(
                    "error", true,
                    "message", "Exception occurred: " + e.getMessage()
                ));
            }
        }
        
        Map<String, Object> response = Map.of(
            "totalAttempts", request.getUserCount(),
            "activeUsers", activeCount,
            "queuedUsers", queuedCount,
            "results", results,
            "queueStats", queueService.getQueueStats(),
            "message", String.format("Concurrent test completed. %d active, %d queued out of %d attempts", 
                activeCount, queuedCount, request.getUserCount())
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test queue processing under load
     * POST /api/queue/test/load-test
     * Body: {"userCount": 50, "processingRounds": 5}
     */
    @PostMapping("/load-test")
    public ResponseEntity<Map<String, Object>> testQueueLoadProcessing(@RequestBody LoadTestRequest request) {
        log.info("Starting queue load test with {} users and {} processing rounds", 
            request.getUserCount(), request.getProcessingRounds());
        
        List<Map<String, Object>> rounds = new ArrayList<>();
        
        // Initial queue stats
        VirtualQueueService.QueueStats initialStats = queueService.getQueueStats();
        
        // Simulate traffic spike
        queueService.simulateTrafficSpike(request.getUserCount());
        
        // Process queue in multiple rounds
        for (int round = 1; round <= request.getProcessingRounds(); round++) {
            VirtualQueueService.QueueStats beforeStats = queueService.getQueueStats();
            
            int processed = queueService.processQueue(10); // Process 10 users per round
            
            VirtualQueueService.QueueStats afterStats = queueService.getQueueStats();
            
            rounds.add(Map.of(
                "round", round,
                "beforeProcessing", Map.of(
                    "queueSize", beforeStats.getTotalInQueue(),
                    "activeUsers", beforeStats.getActiveUsers()
                ),
                "processed", processed,
                "afterProcessing", Map.of(
                    "queueSize", afterStats.getTotalInQueue(),
                    "activeUsers", afterStats.getActiveUsers()
                )
            ));
            
            // Small delay between rounds
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        VirtualQueueService.QueueStats finalStats = queueService.getQueueStats();
        
        Map<String, Object> response = Map.of(
            "userCount", request.getUserCount(),
            "processingRounds", request.getProcessingRounds(),
            "initialStats", Map.of(
                "queueSize", initialStats.getTotalInQueue(),
                "activeUsers", initialStats.getActiveUsers()
            ),
            "finalStats", Map.of(
                "queueSize", finalStats.getTotalInQueue(),
                "activeUsers", finalStats.getActiveUsers()
            ),
            "processingRounds", rounds,
            "summary", String.format("Load test completed. Queue reduced from %d to %d users", 
                initialStats.getTotalInQueue() + request.getUserCount(), finalStats.getTotalInQueue())
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reset queue for testing
     * DELETE /api/queue/test/reset
     */
    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetQueue() {
        log.info("Resetting virtual queue for testing");
        
        // Note: In a real implementation, you would clear Redis keys
        // For now, we'll just return current stats
        VirtualQueueService.QueueStats stats = queueService.getQueueStats();
        
        Map<String, Object> response = Map.of(
            "message", "Queue reset requested",
            "currentStats", Map.of(
                "queueSize", stats.getTotalInQueue(),
                "activeUsers", stats.getActiveUsers()
            ),
            "note", "In production, this would clear all Redis queue keys"
        );
        
        return ResponseEntity.ok(response);
    }
    
    // Request DTOs
    
    public static class EndToEndTestRequest {
        private String userId;
        private String seatNumber;
        
        public EndToEndTestRequest() {}
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    }
    
    public static class ConcurrentTestRequest {
        private int userCount = 10;
        
        public ConcurrentTestRequest() {}
        
        public int getUserCount() { return userCount; }
        public void setUserCount(int userCount) { this.userCount = userCount; }
    }
    
    public static class LoadTestRequest {
        private int userCount = 50;
        private int processingRounds = 5;
        
        public LoadTestRequest() {}
        
        public int getUserCount() { return userCount; }
        public void setUserCount(int userCount) { this.userCount = userCount; }
        
        public int getProcessingRounds() { return processingRounds; }
        public void setProcessingRounds(int processingRounds) { this.processingRounds = processingRounds; }
    }
}
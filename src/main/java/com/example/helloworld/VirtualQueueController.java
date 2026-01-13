package com.example.helloworld;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
@Slf4j
public class VirtualQueueController {
    
    private final VirtualQueueService queueService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, SseEmitter> activeStreams = new ConcurrentHashMap<>();
    
    /**
     * Join the virtual queue
     * POST /api/queue/join
     * Body: {"userId": "user123"}
     */
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinQueue(@RequestBody JoinQueueRequest request) {
        log.info("Queue join request from user: {}", request.getUserId());
        
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "userId is required"
            ));
        }
        
        VirtualQueueService.QueueResult result = queueService.joinQueue(request.getUserId());
        
        Map<String, Object> response = Map.of(
            "success", result.isSuccess(),
            "token", result.getToken() != null ? result.getToken() : "",
            "status", result.getStatus(),
            "position", result.getPosition() != null ? result.getPosition() : 0,
            "estimatedWait", result.getEstimatedWait() != null ? result.getEstimatedWait() : "0 minutes",
            "message", result.getMessage(),
            "userId", request.getUserId()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check queue status
     * GET /api/queue/status/{token}
     */
    @GetMapping("/status/{token}")
    public ResponseEntity<Map<String, Object>> checkQueueStatus(@PathVariable String token) {
        log.debug("Checking queue status for token: {}", token);
        
        VirtualQueueService.QueueStatus status = queueService.checkQueueStatus(token);
        
        Map<String, Object> response = Map.of(
            "token", token,
            "status", status.getStatus(),
            "position", status.getPosition() != null ? status.getPosition() : 0,
            "estimatedWait", status.getEstimatedWait() != null ? status.getEstimatedWait() : "0 minutes",
            "totalInQueue", status.getTotalInQueue() != null ? status.getTotalInQueue() : 0,
            "message", status.getMessage(),
            "timestamp", System.currentTimeMillis()
        );
        
        HttpStatus httpStatus = switch (status.getStatus()) {
            case "ACTIVE" -> HttpStatus.OK;
            case "WAITING" -> HttpStatus.ACCEPTED;
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.OK;
        };
        
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    /**
     * Real-time queue position updates via Server-Sent Events
     * GET /api/queue/stream/{token}
     */
    @GetMapping(value = "/stream/{token}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQueueUpdates(@PathVariable String token) {
        log.info("Starting SSE stream for token: {}", token);
        
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        activeStreams.put(token, emitter);
        
        // Send initial status
        sendQueueUpdate(token, emitter);
        
        // Schedule periodic updates every 10 seconds
        scheduler.scheduleAtFixedRate(() -> {
            if (activeStreams.containsKey(token)) {
                sendQueueUpdate(token, emitter);
            }
        }, 10, 10, TimeUnit.SECONDS);
        
        // Cleanup on completion/timeout
        emitter.onCompletion(() -> {
            activeStreams.remove(token);
            log.debug("SSE stream completed for token: {}", token);
        });
        
        emitter.onTimeout(() -> {
            activeStreams.remove(token);
            log.debug("SSE stream timed out for token: {}", token);
        });
        
        emitter.onError((ex) -> {
            activeStreams.remove(token);
            log.error("SSE stream error for token: {}", token, ex);
        });
        
        return emitter;
    }
    
    /**
     * Get queue statistics (admin endpoint)
     * GET /api/queue/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQueueStats() {
        VirtualQueueService.QueueStats stats = queueService.getQueueStats();
        
        Map<String, Object> response = Map.of(
            "totalInQueue", stats.getTotalInQueue(),
            "activeUsers", stats.getActiveUsers(),
            "maxActiveUsers", stats.getMaxActiveUsers(),
            "processingRatePerMinute", stats.getProcessingRatePerMinute(),
            "queueActive", stats.isQueueActive(),
            "availableSlots", Math.max(0, stats.getMaxActiveUsers() - stats.getActiveUsers()),
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Process queue manually (admin endpoint)
     * POST /api/queue/process
     * Body: {"batchSize": 10} (optional, default: 5)
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processQueue(@RequestBody(required = false) ProcessQueueRequest request) {
        int batchSize = (request != null && request.getBatchSize() > 0) ? request.getBatchSize() : 5;
        
        log.info("Manual queue processing requested, batch size: {}", batchSize);
        
        int processed = queueService.processQueue(batchSize);
        
        Map<String, Object> response = Map.of(
            "processed", processed,
            "batchSize", batchSize,
            "message", String.format("Processed %d users from queue", processed),
            "timestamp", System.currentTimeMillis()
        );
        
        // Notify all active streams about queue updates
        notifyAllStreams();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Simulate traffic spike for testing
     * POST /api/queue/simulate-traffic
     * Body: {"userCount": 50}
     */
    @PostMapping("/simulate-traffic")
    public ResponseEntity<Map<String, Object>> simulateTraffic(@RequestBody SimulateTrafficRequest request) {
        if (request.getUserCount() <= 0 || request.getUserCount() > 1000) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "userCount must be between 1 and 1000"
            ));
        }
        
        log.info("Simulating traffic spike with {} users", request.getUserCount());
        
        // Run simulation in background
        scheduler.execute(() -> {
            queueService.simulateTrafficSpike(request.getUserCount());
            notifyAllStreams();
        });
        
        Map<String, Object> response = Map.of(
            "success", true,
            "message", String.format("Simulating traffic spike with %d users", request.getUserCount()),
            "userCount", request.getUserCount(),
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Remove user from active session (for testing/admin)
     * DELETE /api/queue/active/{userId}
     */
    @DeleteMapping("/active/{userId}")
    public ResponseEntity<Map<String, Object>> removeActiveUser(@PathVariable String userId) {
        log.info("Removing active user: {}", userId);
        
        queueService.removeActiveUser(userId);
        
        Map<String, Object> response = Map.of(
            "success", true,
            "message", String.format("User %s removed from active session", userId),
            "userId", userId,
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }
    
    // Private helper methods
    
    private void sendQueueUpdate(String token, SseEmitter emitter) {
        try {
            VirtualQueueService.QueueStatus status = queueService.checkQueueStatus(token);
            
            Map<String, Object> data = Map.of(
                "status", status.getStatus(),
                "position", status.getPosition() != null ? status.getPosition() : 0,
                "estimatedWait", status.getEstimatedWait() != null ? status.getEstimatedWait() : "0 minutes",
                "totalInQueue", status.getTotalInQueue() != null ? status.getTotalInQueue() : 0,
                "message", status.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            emitter.send(SseEmitter.event()
                .name("queue-update")
                .data(data));
            
            // If user is now active, complete the stream
            if ("ACTIVE".equals(status.getStatus())) {
                emitter.send(SseEmitter.event()
                    .name("admitted")
                    .data(Map.of("message", "You can now start booking!")));
                emitter.complete();
                activeStreams.remove(token);
            }
            
        } catch (IOException e) {
            log.error("Error sending SSE update for token: {}", token, e);
            emitter.completeWithError(e);
            activeStreams.remove(token);
        }
    }
    
    private void notifyAllStreams() {
        activeStreams.forEach((token, emitter) -> {
            scheduler.execute(() -> sendQueueUpdate(token, emitter));
        });
    }
    
    // Request DTOs
    
    public static class JoinQueueRequest {
        private String userId;
        
        public JoinQueueRequest() {}
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
    
    public static class ProcessQueueRequest {
        private int batchSize = 5;
        
        public ProcessQueueRequest() {}
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }
    
    public static class SimulateTrafficRequest {
        private int userCount;
        
        public SimulateTrafficRequest() {}
        
        public int getUserCount() { return userCount; }
        public void setUserCount(int userCount) { this.userCount = userCount; }
    }
}
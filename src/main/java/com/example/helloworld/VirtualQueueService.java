package com.example.helloworld;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirtualQueueService {
    
    private final StringRedisTemplate redisTemplate;
    
    // Redis keys
    private static final String QUEUE_KEY = "virtual_queue:waiting";
    private static final String ACTIVE_USERS_KEY = "virtual_queue:active";
    private static final String USER_TOKEN_KEY = "virtual_queue:user_tokens:";
    private static final String TOKEN_USER_KEY = "virtual_queue:token_users:";
    private static final String QUEUE_CONFIG_KEY = "virtual_queue:config";
    
    // Configuration
    private static final int MAX_ACTIVE_USERS = 100; // Max concurrent users allowed in booking system
    private static final int PROCESSING_RATE_PER_MINUTE = 20; // Users processed from queue per minute
    private static final long TOKEN_EXPIRY_HOURS = 2; // Token validity
    
    /**
     * Join the virtual queue during high traffic
     * @param userId User identifier
     * @return QueueResult with token and position
     */
    public QueueResult joinQueue(String userId) {
        log.info("User {} attempting to join virtual queue", userId);
        
        // Check if user is already in active session
        if (isUserActive(userId)) {
            String existingToken = getActiveUserToken(userId);
            return QueueResult.alreadyActive(existingToken, "You already have an active booking session");
        }
        
        // Check if user is already in queue
        String existingToken = getUserQueueToken(userId);
        if (existingToken != null) {
            Long position = getQueuePosition(existingToken);
            if (position != null) {
                return QueueResult.alreadyInQueue(existingToken, position, calculateEstimatedWait(position));
            }
        }
        
        // Check if we can admit user directly (queue not full)
        long activeCount = getActiveUserCount();
        if (activeCount < MAX_ACTIVE_USERS) {
            String token = admitUserDirectly(userId);
            log.info("User {} admitted directly with token {}", userId, token);
            return QueueResult.admittedDirectly(token, "Welcome! You can start booking immediately");
        }
        
        // Add user to queue
        String queueToken = generateQueueToken();
        double score = Instant.now().toEpochMilli(); // FIFO based on timestamp
        
        redisTemplate.opsForZSet().add(QUEUE_KEY, queueToken, score);
        
        // Store user-token mappings
        redisTemplate.opsForValue().set(USER_TOKEN_KEY + userId, queueToken, TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(TOKEN_USER_KEY + queueToken, userId, TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);
        
        Long position = getQueuePosition(queueToken);
        String estimatedWait = calculateEstimatedWait(position != null ? position : 0);
        
        log.info("User {} added to queue at position {} with token {}", userId, position, queueToken);
        
        return QueueResult.addedToQueue(queueToken, position != null ? position : 0, estimatedWait);
    }
    
    /**
     * Check user's position in queue
     * @param token Queue token
     * @return QueueStatus with current position and wait time
     */
    public QueueStatus checkQueueStatus(String token) {
        // Check if user is now active
        String userId = getUserFromToken(token);
        if (userId != null && isUserActive(userId)) {
            return QueueStatus.active("Your session is active! You can start booking now");
        }
        
        // Check position in queue
        Long position = getQueuePosition(token);
        if (position == null) {
            return QueueStatus.notFound("Token not found or expired");
        }
        
        String estimatedWait = calculateEstimatedWait(position);
        long totalInQueue = redisTemplate.opsForZSet().count(QUEUE_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        
        return QueueStatus.waiting(position, estimatedWait, totalInQueue);
    }
    
    /**
     * Process queue - admit next batch of users (called by scheduler)
     * @param batchSize Number of users to process
     * @return Number of users actually processed
     */
    public int processQueue(int batchSize) {
        long activeCount = getActiveUserCount();
        int availableSlots = (int) Math.max(0, MAX_ACTIVE_USERS - activeCount);
        int toProcess = Math.min(batchSize, availableSlots);
        
        if (toProcess <= 0) {
            log.debug("No available slots for queue processing. Active users: {}", activeCount);
            return 0;
        }
        
        // Get next users from queue (lowest scores = earliest timestamps)
        Set<String> nextUsers = redisTemplate.opsForZSet().range(QUEUE_KEY, 0, toProcess - 1);
        
        if (nextUsers == null || nextUsers.isEmpty()) {
            log.debug("No users in queue to process");
            return 0;
        }
        
        int processed = 0;
        for (String token : nextUsers) {
            String userId = getUserFromToken(token);
            if (userId != null) {
                // Move user from queue to active
                String activeToken = admitUserFromQueue(userId, token);
                if (activeToken != null) {
                    processed++;
                    log.info("Processed user {} from queue, new active token: {}", userId, activeToken);
                }
            }
        }
        
        log.info("Processed {} users from virtual queue", processed);
        return processed;
    }
    
    /**
     * Remove user from active session (when they complete booking or session expires)
     * @param userId User identifier
     */
    public void removeActiveUser(String userId) {
        String token = getActiveUserToken(userId);
        if (token != null) {
            redisTemplate.opsForSet().remove(ACTIVE_USERS_KEY, token);
            redisTemplate.delete(USER_TOKEN_KEY + userId);
            redisTemplate.delete(TOKEN_USER_KEY + token);
            log.info("Removed user {} from active session", userId);
        }
    }
    
    /**
     * Get queue statistics for monitoring
     */
    public QueueStats getQueueStats() {
        long totalInQueue = redisTemplate.opsForZSet().count(QUEUE_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        long activeUsers = getActiveUserCount();
        
        return new QueueStats(totalInQueue, activeUsers, MAX_ACTIVE_USERS, PROCESSING_RATE_PER_MINUTE);
    }
    
    /**
     * Simulate high traffic for testing
     */
    public void simulateTrafficSpike(int userCount) {
        log.info("Simulating traffic spike with {} users", userCount);
        
        for (int i = 1; i <= userCount; i++) {
            String userId = "spike_user_" + i;
            joinQueue(userId);
        }
        
        log.info("Traffic spike simulation completed");
    }
    
    // Private helper methods
    
    private boolean isUserActive(String userId) {
        String token = getActiveUserToken(userId);
        return token != null && redisTemplate.opsForSet().isMember(ACTIVE_USERS_KEY, token);
    }
    
    private String getActiveUserToken(String userId) {
        return redisTemplate.opsForValue().get(USER_TOKEN_KEY + userId);
    }
    
    private String getUserQueueToken(String userId) {
        return redisTemplate.opsForValue().get(USER_TOKEN_KEY + userId);
    }
    
    private String getUserFromToken(String token) {
        return redisTemplate.opsForValue().get(TOKEN_USER_KEY + token);
    }
    
    private Long getQueuePosition(String token) {
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, token);
        return rank != null ? rank + 1 : null; // Convert 0-based to 1-based
    }
    
    private long getActiveUserCount() {
        return redisTemplate.opsForSet().size(ACTIVE_USERS_KEY);
    }
    
    private String admitUserDirectly(String userId) {
        String token = generateActiveToken();
        
        redisTemplate.opsForSet().add(ACTIVE_USERS_KEY, token);
        redisTemplate.opsForValue().set(USER_TOKEN_KEY + userId, token, TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(TOKEN_USER_KEY + token, userId, TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);
        
        return token;
    }
    
    private String admitUserFromQueue(String userId, String queueToken) {
        // Remove from queue
        redisTemplate.opsForZSet().remove(QUEUE_KEY, queueToken);
        
        // Add to active users
        String activeToken = generateActiveToken();
        redisTemplate.opsForSet().add(ACTIVE_USERS_KEY, activeToken);
        
        // Update token mappings
        redisTemplate.opsForValue().set(USER_TOKEN_KEY + userId, activeToken, TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(TOKEN_USER_KEY + activeToken, userId, TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);
        
        // Clean up old queue token mapping
        redisTemplate.delete(TOKEN_USER_KEY + queueToken);
        
        return activeToken;
    }
    
    private String calculateEstimatedWait(long position) {
        if (position <= 0) return "0 minutes";
        
        double waitMinutes = (double) position / PROCESSING_RATE_PER_MINUTE;
        
        if (waitMinutes < 1) {
            return "Less than 1 minute";
        } else if (waitMinutes < 60) {
            return String.format("%.0f minutes", waitMinutes);
        } else {
            double hours = waitMinutes / 60;
            return String.format("%.1f hours", hours);
        }
    }
    
    private String generateQueueToken() {
        return "queue_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    private String generateActiveToken() {
        return "active_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    // Result classes
    
    public static class QueueResult {
        private final boolean success;
        private final String token;
        private final Long position;
        private final String estimatedWait;
        private final String message;
        private final String status; // "ACTIVE", "QUEUED", "ALREADY_ACTIVE", "ALREADY_QUEUED"
        
        private QueueResult(boolean success, String token, Long position, String estimatedWait, String message, String status) {
            this.success = success;
            this.token = token;
            this.position = position;
            this.estimatedWait = estimatedWait;
            this.message = message;
            this.status = status;
        }
        
        public static QueueResult admittedDirectly(String token, String message) {
            return new QueueResult(true, token, 0L, "0 minutes", message, "ACTIVE");
        }
        
        public static QueueResult addedToQueue(String token, long position, String estimatedWait) {
            return new QueueResult(true, token, position, estimatedWait, 
                "Added to queue at position " + position, "QUEUED");
        }
        
        public static QueueResult alreadyActive(String token, String message) {
            return new QueueResult(true, token, 0L, "0 minutes", message, "ALREADY_ACTIVE");
        }
        
        public static QueueResult alreadyInQueue(String token, long position, String estimatedWait) {
            return new QueueResult(true, token, position, estimatedWait, 
                "Already in queue at position " + position, "ALREADY_QUEUED");
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getToken() { return token; }
        public Long getPosition() { return position; }
        public String getEstimatedWait() { return estimatedWait; }
        public String getMessage() { return message; }
        public String getStatus() { return status; }
    }
    
    public static class QueueStatus {
        private final String status; // "WAITING", "ACTIVE", "NOT_FOUND"
        private final Long position;
        private final String estimatedWait;
        private final Long totalInQueue;
        private final String message;
        
        private QueueStatus(String status, Long position, String estimatedWait, Long totalInQueue, String message) {
            this.status = status;
            this.position = position;
            this.estimatedWait = estimatedWait;
            this.totalInQueue = totalInQueue;
            this.message = message;
        }
        
        public static QueueStatus waiting(long position, String estimatedWait, long totalInQueue) {
            return new QueueStatus("WAITING", position, estimatedWait, totalInQueue, 
                "You are number " + position + " in line");
        }
        
        public static QueueStatus active(String message) {
            return new QueueStatus("ACTIVE", 0L, "0 minutes", 0L, message);
        }
        
        public static QueueStatus notFound(String message) {
            return new QueueStatus("NOT_FOUND", null, null, null, message);
        }
        
        // Getters
        public String getStatus() { return status; }
        public Long getPosition() { return position; }
        public String getEstimatedWait() { return estimatedWait; }
        public Long getTotalInQueue() { return totalInQueue; }
        public String getMessage() { return message; }
    }
    
    public static class QueueStats {
        private final long totalInQueue;
        private final long activeUsers;
        private final int maxActiveUsers;
        private final int processingRatePerMinute;
        
        public QueueStats(long totalInQueue, long activeUsers, int maxActiveUsers, int processingRatePerMinute) {
            this.totalInQueue = totalInQueue;
            this.activeUsers = activeUsers;
            this.maxActiveUsers = maxActiveUsers;
            this.processingRatePerMinute = processingRatePerMinute;
        }
        
        // Getters
        public long getTotalInQueue() { return totalInQueue; }
        public long getActiveUsers() { return activeUsers; }
        public int getMaxActiveUsers() { return maxActiveUsers; }
        public int getProcessingRatePerMinute() { return processingRatePerMinute; }
        public boolean isQueueActive() { return totalInQueue > 0 || activeUsers >= maxActiveUsers; }
    }
}
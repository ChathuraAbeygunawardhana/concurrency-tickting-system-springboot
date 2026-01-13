package com.example.helloworld;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {
    
    private final StringRedisTemplate stringRedisTemplate;
    private static final String LOCK_PREFIX = "seat_lock:";
    
    /**
     * Acquires a distributed lock for a seat
     * @param seatId The seat identifier
     * @param userId The user attempting to lock the seat
     * @param ttlSeconds Time-to-live for the lock in seconds
     * @return true if lock acquired successfully, false otherwise
     */
    public boolean acquireLock(String seatId, String userId, long ttlSeconds) {
        String lockKey = LOCK_PREFIX + seatId;
        
        try {
            // Use setIfAbsent for atomic operation - only sets if key doesn't exist
            Boolean lockAcquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, userId, Duration.ofSeconds(ttlSeconds));
            
            if (Boolean.TRUE.equals(lockAcquired)) {
                log.info("Lock acquired for seat: {} by user: {} for {} seconds", seatId, userId, ttlSeconds);
                return true;
            } else {
                String currentLockHolder = stringRedisTemplate.opsForValue().get(lockKey);
                log.warn("Failed to acquire lock for seat: {} by user: {}. Current holder: {}", 
                    seatId, userId, currentLockHolder);
                return false;
            }
        } catch (Exception e) {
            log.error("Error acquiring lock for seat: {} by user: {}", seatId, userId, e);
            return false;
        }
    }
    
    /**
     * Releases the distributed lock for a seat
     * @param seatId The seat identifier
     */
    public void releaseLock(String seatId) {
        String lockKey = LOCK_PREFIX + seatId;
        
        try {
            Boolean deleted = stringRedisTemplate.delete(lockKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Lock released for seat: {}", seatId);
            } else {
                log.warn("No lock found to release for seat: {}", seatId);
            }
        } catch (Exception e) {
            log.error("Error releasing lock for seat: {}", seatId, e);
        }
    }
    
    /**
     * Checks if a seat is currently locked
     * @param seatId The seat identifier
     * @return true if locked, false otherwise
     */
    public boolean isLocked(String seatId) {
        String lockKey = LOCK_PREFIX + seatId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey));
    }
    
    /**
     * Gets the current lock holder for a seat
     * @param seatId The seat identifier
     * @return userId of lock holder, or null if not locked
     */
    public String getLockHolder(String seatId) {
        String lockKey = LOCK_PREFIX + seatId;
        return stringRedisTemplate.opsForValue().get(lockKey);
    }
}
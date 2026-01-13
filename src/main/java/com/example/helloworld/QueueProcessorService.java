package com.example.helloworld;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueProcessorService {
    
    private final VirtualQueueService virtualQueueService;
    
    /**
     * Automatically process the virtual queue every 30 seconds
     * This simulates the natural flow of users completing their bookings
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void processQueueAutomatically() {
        try {
            VirtualQueueService.QueueStats stats = virtualQueueService.getQueueStats();
            
            // Only process if there are users in queue and available slots
            if (stats.getTotalInQueue() > 0 && stats.getActiveUsers() < stats.getMaxActiveUsers()) {
                // Process 3-5 users per batch (simulating natural completion rate)
                int batchSize = Math.min(3, (int) Math.min(stats.getTotalInQueue(), 
                    stats.getMaxActiveUsers() - stats.getActiveUsers()));
                
                if (batchSize > 0) {
                    int processed = virtualQueueService.processQueue(batchSize);
                    if (processed > 0) {
                        log.info("Auto-processed {} users from queue. Queue size: {}, Active: {}", 
                            processed, stats.getTotalInQueue(), stats.getActiveUsers());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in automatic queue processing", e);
        }
    }
    
    /**
     * Clean up expired active sessions every 5 minutes
     * This simulates users who abandon their booking sessions
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredSessions() {
        try {
            // In a real implementation, you would check for expired tokens
            // and remove them from active users. For now, we'll just log.
            VirtualQueueService.QueueStats stats = virtualQueueService.getQueueStats();
            log.debug("Session cleanup check - Active users: {}, Queue size: {}", 
                stats.getActiveUsers(), stats.getTotalInQueue());
            
            // TODO: Implement actual session expiry cleanup
            // This would involve checking token timestamps and removing expired ones
        } catch (Exception e) {
            log.error("Error in session cleanup", e);
        }
    }
}
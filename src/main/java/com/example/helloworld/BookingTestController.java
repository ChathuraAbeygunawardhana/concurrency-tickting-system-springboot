package com.example.helloworld;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class BookingTestController {
    
    private final BookingService bookingService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    /**
     * Test endpoint to simulate concurrent booking attempts
     * GET /api/test/concurrent-booking?seatNumber=A1&userCount=5
     */
    @GetMapping("/concurrent-booking")
    public ResponseEntity<Map<String, Object>> testConcurrentBooking(
            @RequestParam String seatNumber,
            @RequestParam(defaultValue = "3") int userCount) {
        
        log.info("Testing concurrent booking for seat: {} with {} users", seatNumber, userCount);
        
        List<CompletableFuture<BookingService.BookingResult>> futures = new ArrayList<>();
        
        // Create multiple concurrent booking attempts
        for (int i = 1; i <= userCount; i++) {
            String userId = "testUser" + i;
            CompletableFuture<BookingService.BookingResult> future = CompletableFuture
                .supplyAsync(() -> bookingService.bookSeat(seatNumber, userId), executorService);
            futures.add(future);
        }
        
        // Wait for all attempts to complete
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (int i = 0; i < futures.size(); i++) {
            try {
                BookingService.BookingResult result = futures.get(i).get();
                String userId = "testUser" + (i + 1);
                
                Map<String, Object> resultMap = Map.of(
                    "userId", userId,
                    "success", result.isSuccess(),
                    "message", result.getMessage(),
                    "bookingId", result.getBookingId() != null ? result.getBookingId() : ""
                );
                
                results.add(resultMap);
                
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failureCount++;
                }
                
            } catch (Exception e) {
                log.error("Error in concurrent booking test", e);
                results.add(Map.of(
                    "userId", "testUser" + (i + 1),
                    "success", false,
                    "message", "Exception occurred: " + e.getMessage(),
                    "bookingId", ""
                ));
                failureCount++;
            }
        }
        
        Map<String, Object> response = Map.of(
            "seatNumber", seatNumber,
            "totalAttempts", userCount,
            "successfulBookings", successCount,
            "failedBookings", failureCount,
            "results", results,
            "message", String.format("Concurrent booking test completed. %d successful, %d failed out of %d attempts", 
                successCount, failureCount, userCount)
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test endpoint to check system health and connectivity
     * GET /api/test/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            // Test seat status check (tests DB connectivity)
            BookingService.SeatStatusResult seatStatus = bookingService.getSeatStatus("A1");
            
            Map<String, Object> response = Map.of(
                "status", "healthy",
                "database", "connected",
                "redis", "connected",
                "sampleSeatCheck", Map.of(
                    "seatNumber", seatStatus.getSeatNumber(),
                    "status", seatStatus.getStatus() != null ? seatStatus.getStatus().toString() : "NOT_FOUND",
                    "locked", seatStatus.isLocked()
                ),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            
            Map<String, Object> response = Map.of(
                "status", "unhealthy",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Test endpoint to simulate the complete booking workflow step by step
     * POST /api/test/booking-workflow
     * Body: {"seatNumber": "A1", "userId": "testUser"}
     */
    @PostMapping("/booking-workflow")
    public ResponseEntity<Map<String, Object>> testBookingWorkflow(@RequestBody Map<String, String> request) {
        String seatNumber = request.get("seatNumber");
        String userId = request.get("userId");
        
        if (seatNumber == null || userId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "seatNumber and userId are required"
            ));
        }
        
        log.info("Testing complete booking workflow for seat: {} by user: {}", seatNumber, userId);
        
        List<Map<String, Object>> steps = new ArrayList<>();
        
        // Step 1: Check initial seat status
        BookingService.SeatStatusResult initialStatus = bookingService.getSeatStatus(seatNumber);
        steps.add(Map.of(
            "step", 1,
            "description", "Check initial seat status",
            "result", Map.of(
                "seatNumber", initialStatus.getSeatNumber(),
                "status", initialStatus.getStatus() != null ? initialStatus.getStatus().toString() : "NOT_FOUND",
                "locked", initialStatus.isLocked(),
                "message", initialStatus.getMessage()
            )
        ));
        
        // Step 2: Attempt booking
        BookingService.BookingResult bookingResult = bookingService.bookSeat(seatNumber, userId);
        steps.add(Map.of(
            "step", 2,
            "description", "Attempt seat booking",
            "result", Map.of(
                "success", bookingResult.isSuccess(),
                "message", bookingResult.getMessage(),
                "bookingId", bookingResult.getBookingId() != null ? bookingResult.getBookingId() : ""
            )
        ));
        
        // Step 3: Check final seat status
        BookingService.SeatStatusResult finalStatus = bookingService.getSeatStatus(seatNumber);
        steps.add(Map.of(
            "step", 3,
            "description", "Check final seat status",
            "result", Map.of(
                "seatNumber", finalStatus.getSeatNumber(),
                "status", finalStatus.getStatus() != null ? finalStatus.getStatus().toString() : "NOT_FOUND",
                "locked", finalStatus.isLocked(),
                "message", finalStatus.getMessage()
            )
        ));
        
        Map<String, Object> response = Map.of(
            "seatNumber", seatNumber,
            "userId", userId,
            "overallSuccess", bookingResult.isSuccess(),
            "steps", steps,
            "summary", bookingResult.isSuccess() ? 
                "Booking workflow completed successfully" : 
                "Booking workflow failed: " + bookingResult.getMessage()
        );
        
        return ResponseEntity.ok(response);
    }
}
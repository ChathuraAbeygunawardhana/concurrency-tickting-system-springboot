package com.example.helloworld;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final RedisLockService redisLockService;
    private final MockPaymentService mockPaymentService;
    private final VirtualQueueService virtualQueueService;
    
    private static final long LOCK_TTL_SECONDS = 300; // 5 minutes
    private static final double TICKET_PRICE = 50.0;
    
    /**
     * Attempts to book a seat following the complete workflow:
     * 1. Check if user has active queue session
     * 2. Acquire Redis lock
     * 3. Check seat availability in DB
     * 4. Process payment
     * 5. Update seat status and create booking
     * 6. Release lock and remove user from active session
     */
    @Transactional
    public BookingResult bookSeat(String seatNumber, String userId) {
        log.info("Starting booking process for seat: {} by user: {}", seatNumber, userId);
        
        // Check if virtual queue is active and user has permission
        VirtualQueueService.QueueStats queueStats = virtualQueueService.getQueueStats();
        if (queueStats.isQueueActive()) {
            // Check if user has an active session token
            VirtualQueueService.QueueStatus userStatus = virtualQueueService.checkQueueStatus(userId + "_token");
            if (!"ACTIVE".equals(userStatus.getStatus())) {
                return BookingResult.failure("Virtual queue is active. Please join the queue first at /api/queue/join");
            }
        }
        
        // Step 1: Try to acquire Redis lock
        if (!redisLockService.acquireLock(seatNumber, userId, LOCK_TTL_SECONDS)) {
            log.warn("Failed to acquire lock for seat: {} - seat is currently being processed", seatNumber);
            return BookingResult.failure("Seat is currently being processed by another user");
        }
        
        try {
            // Step 2: Check seat availability in database
            Optional<Seat> seatOpt = seatRepository.findBySeatNumber(seatNumber);
            if (seatOpt.isEmpty()) {
                return BookingResult.failure("Seat not found");
            }
            
            Seat seat = seatOpt.get();
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                return BookingResult.failure("Seat is not available");
            }
            
            // Step 3: Create pending booking
            Booking booking = new Booking();
            booking.setUserId(userId);
            booking.setSeatId(seat.getId());
            booking.setStatus(BookingStatus.PENDING);
            booking.setBookingTime(LocalDateTime.now());
            booking = bookingRepository.save(booking);
            
            // Step 4: Process payment (simulates network latency and random failures)
            boolean paymentSuccess = mockPaymentService.processPayment(userId, TICKET_PRICE);
            
            if (!paymentSuccess) {
                // Payment failed - update booking status and return failure
                booking.setStatus(BookingStatus.FAILED);
                bookingRepository.save(booking);
                return BookingResult.failure("Payment processing failed");
            }
            
            // Step 5: Payment successful - update seat status and confirm booking
            seat.setStatus(SeatStatus.SOLD);
            booking.setStatus(BookingStatus.CONFIRMED);
            
            // Save both entities (optimistic locking will prevent race conditions)
            seatRepository.save(seat);
            bookingRepository.save(booking);
            
            log.info("Booking successful for seat: {} by user: {} - Booking ID: {}", 
                seatNumber, userId, booking.getId());
            
            // Step 6: Remove user from virtual queue active session
            if (queueStats.isQueueActive()) {
                virtualQueueService.removeActiveUser(userId);
                log.info("Removed user {} from virtual queue active session after successful booking", userId);
            }
            
            return BookingResult.success(booking.getId(), "Booking confirmed successfully");
            
        } catch (Exception e) {
            log.error("Error during booking process for seat: {} by user: {}", seatNumber, userId, e);
            
            // Try to find and update any pending booking to failed status
            try {
                Optional<Seat> seatOpt = seatRepository.findBySeatNumber(seatNumber);
                if (seatOpt.isPresent()) {
                    Optional<Booking> pendingBooking = bookingRepository
                        .findBySeatIdAndStatus(seatOpt.get().getId(), BookingStatus.PENDING);
                    if (pendingBooking.isPresent()) {
                        Booking booking = pendingBooking.get();
                        booking.setStatus(BookingStatus.FAILED);
                        bookingRepository.save(booking);
                    }
                }
            } catch (Exception rollbackError) {
                log.error("Error during rollback for seat: {}", seatNumber, rollbackError);
            }
            
            return BookingResult.failure("Internal error occurred during booking");
            
        } finally {
            // Step 7: Always release the lock
            redisLockService.releaseLock(seatNumber);
        }
    }
    
    /**
     * Gets the current status of a seat
     */
    public SeatStatusResult getSeatStatus(String seatNumber) {
        Optional<Seat> seatOpt = seatRepository.findBySeatNumber(seatNumber);
        if (seatOpt.isEmpty()) {
            return new SeatStatusResult(seatNumber, null, false, "Seat not found");
        }
        
        Seat seat = seatOpt.get();
        boolean isLocked = redisLockService.isLocked(seatNumber);
        String lockHolder = redisLockService.getLockHolder(seatNumber);
        
        return new SeatStatusResult(seatNumber, seat.getStatus(), isLocked, 
            isLocked ? "Locked by user: " + lockHolder : "Available for booking");
    }
    
    /**
     * Result class for booking operations
     */
    public static class BookingResult {
        private final boolean success;
        private final String message;
        private final Long bookingId;
        
        private BookingResult(boolean success, String message, Long bookingId) {
            this.success = success;
            this.message = message;
            this.bookingId = bookingId;
        }
        
        public static BookingResult success(Long bookingId, String message) {
            return new BookingResult(true, message, bookingId);
        }
        
        public static BookingResult failure(String message) {
            return new BookingResult(false, message, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getBookingId() { return bookingId; }
    }
    
    /**
     * Result class for seat status queries
     */
    public static class SeatStatusResult {
        private final String seatNumber;
        private final SeatStatus status;
        private final boolean locked;
        private final String message;
        
        public SeatStatusResult(String seatNumber, SeatStatus status, boolean locked, String message) {
            this.seatNumber = seatNumber;
            this.status = status;
            this.locked = locked;
            this.message = message;
        }
        
        // Getters
        public String getSeatNumber() { return seatNumber; }
        public SeatStatus getStatus() { return status; }
        public boolean isLocked() { return locked; }
        public String getMessage() { return message; }
    }
}
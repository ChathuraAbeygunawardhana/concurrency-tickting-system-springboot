package com.example.helloworld;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class BookingController {
    
    private final BookingService bookingService;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    
    /**
     * Get status of a specific seat
     * GET /api/seats/{seatNumber}/status
     */
    @GetMapping("/seats/{seatNumber}/status")
    public ResponseEntity<Map<String, Object>> getSeatStatus(@PathVariable String seatNumber) {
        log.info("Checking status for seat: {}", seatNumber);
        
        BookingService.SeatStatusResult result = bookingService.getSeatStatus(seatNumber);
        
        Map<String, Object> response = Map.of(
            "seatNumber", result.getSeatNumber(),
            "status", result.getStatus() != null ? result.getStatus().toString() : "NOT_FOUND",
            "locked", result.isLocked(),
            "message", result.getMessage()
        );
        
        if (result.getStatus() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all seats with their current status
     * GET /api/seats
     */
    @GetMapping("/seats")
    public ResponseEntity<List<Map<String, Object>>> getAllSeats() {
        log.info("Fetching all seats");
        
        List<Seat> seats = seatRepository.findAll();
        List<Map<String, Object>> response = seats.stream()
            .map(seat -> {
                BookingService.SeatStatusResult status = bookingService.getSeatStatus(seat.getSeatNumber());
                return Map.<String, Object>of(
                    "id", seat.getId(),
                    "seatNumber", seat.getSeatNumber(),
                    "status", seat.getStatus().toString(),
                    "locked", status.isLocked(),
                    "version", seat.getVersion()
                );
            })
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Book a seat
     * POST /api/bookings
     * Body: {"seatNumber": "A1", "userId": "user123"}
     */
    @PostMapping("/bookings")
    public ResponseEntity<Map<String, Object>> bookSeat(@RequestBody BookingRequest request) {
        log.info("Booking request for seat: {} by user: {}", request.getSeatNumber(), request.getUserId());
        
        if (request.getSeatNumber() == null || request.getUserId() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "seatNumber and userId are required"
            ));
        }
        
        BookingService.BookingResult result = bookingService.bookSeat(request.getSeatNumber(), request.getUserId());
        
        Map<String, Object> response = Map.of(
            "success", result.isSuccess(),
            "message", result.getMessage(),
            "bookingId", result.getBookingId() != null ? result.getBookingId() : "",
            "seatNumber", request.getSeatNumber(),
            "userId", request.getUserId()
        );
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }
    
    /**
     * Get booking details
     * GET /api/bookings/{bookingId}
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<Map<String, Object>> getBooking(@PathVariable Long bookingId) {
        log.info("Fetching booking details for ID: {}", bookingId);
        
        return bookingRepository.findById(bookingId)
            .map(booking -> {
                Map<String, Object> response = Map.of(
                    "id", booking.getId(),
                    "userId", booking.getUserId(),
                    "seatId", booking.getSeatId(),
                    "status", booking.getStatus().toString(),
                    "bookingTime", booking.getBookingTime().toString()
                );
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all bookings for a user
     * GET /api/users/{userId}/bookings
     */
    @GetMapping("/users/{userId}/bookings")
    public ResponseEntity<List<Map<String, Object>>> getUserBookings(@PathVariable String userId) {
        log.info("Fetching bookings for user: {}", userId);
        
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        List<Map<String, Object>> response = bookings.stream()
            .map(booking -> Map.<String, Object>of(
                "id", booking.getId(),
                "seatId", booking.getSeatId(),
                "status", booking.getStatus().toString(),
                "bookingTime", booking.getBookingTime().toString()
            ))
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Request DTO for booking
     */
    public static class BookingRequest {
        private String seatNumber;
        private String userId;
        
        // Constructors
        public BookingRequest() {}
        
        public BookingRequest(String seatNumber, String userId) {
            this.seatNumber = seatNumber;
            this.userId = userId;
        }
        
        // Getters and Setters
        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
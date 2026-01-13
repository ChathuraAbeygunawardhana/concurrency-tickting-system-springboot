package com.example.helloworld;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
@Slf4j
public class MockPaymentService {
    
    private final Random random = new Random();
    
    /**
     * Simulates payment processing with network latency and random failures
     * @param userId The user making the payment
     * @param amount The payment amount
     * @return true if payment succeeds, false if declined (10% chance)
     */
    public boolean processPayment(String userId, double amount) {
        log.info("Processing payment for user: {} amount: ${}", userId, amount);
        
        try {
            // Simulate network latency
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted for user: {}", userId);
            return false;
        }
        
        // 10% chance of payment failure
        boolean paymentSuccess = random.nextInt(100) >= 10;
        
        if (paymentSuccess) {
            log.info("Payment successful for user: {} amount: ${}", userId, amount);
        } else {
            log.warn("Payment declined for user: {} amount: ${}", userId, amount);
        }
        
        return paymentSuccess;
    }
}
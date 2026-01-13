package com.example.helloworld;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserId(String userId);
    
    List<Booking> findBySeatId(Long seatId);
    
    @Query("SELECT b FROM Booking b WHERE b.userId = :userId AND b.status = :status")
    List<Booking> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") BookingStatus status);
    
    Optional<Booking> findBySeatIdAndStatus(Long seatId, BookingStatus status);
}
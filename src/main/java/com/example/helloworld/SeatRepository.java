package com.example.helloworld;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    Optional<Seat> findBySeatNumber(String seatNumber);
    
    @Query("SELECT s FROM Seat s WHERE s.seatNumber = :seatNumber AND s.status = :status")
    Optional<Seat> findBySeatNumberAndStatus(@Param("seatNumber") String seatNumber, @Param("status") SeatStatus status);
}
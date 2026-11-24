
package com.example.ticketreservation.repository;

import com.example.ticketreservation.model.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {

    List<Reservation> findByCustomerId(String customerId);

    List<Reservation> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}

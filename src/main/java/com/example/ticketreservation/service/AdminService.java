
package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.StatsResponse;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;

    public AdminService(
            UserRepository userRepository,
            EventRepository eventRepository,
            ReservationRepository reservationRepository) {

        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
    }

    public StatsResponse getStatistics() {

        long users = userRepository.count();

        long events = eventRepository.count();

        List<Reservation> reservations =
                reservationRepository.findAll();

        long ticketsSold = reservations.stream()
                .filter(reservation ->
                        reservation.getStatus() == ReservationStatus.CONFIRMED)
                .mapToLong(Reservation::getSeats)
                .sum();

        BigDecimal revenue = reservations.stream()
                .filter(reservation ->
                        reservation.getStatus() == ReservationStatus.CONFIRMED)
                .map(Reservation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatsResponse(
                users,
                events,
                ticketsSold,
                revenue
        );
    }
}

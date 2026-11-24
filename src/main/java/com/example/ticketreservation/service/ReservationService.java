
package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventService eventService;

    public ReservationService(
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            EventService eventService) {

        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
    }

    public ReservationResponse createReservation(
            ReservationRequest request) {

        String username = getCurrentUsername();

        AppUser customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found"
                ));

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        eventService.decreaseAvailableSeats(
                event.getId(),
                request.seats()
        );

        try {
            BigDecimal amount = event.getTicketPrice()
                    .multiply(BigDecimal.valueOf(request.seats()));

            Reservation reservation = new Reservation();

            reservation.setReservationCode(
                    "TKT-" + UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase()
            );

            reservation.setCustomerId(customer.getId());
            reservation.setEventId(event.getId());
            reservation.setSeats(request.seats());
            reservation.setAmount(amount);
            reservation.setStatus(ReservationStatus.CONFIRMED);

            return toResponse(reservationRepository.save(reservation));

        } catch (RuntimeException exception) {

            eventService.increaseAvailableSeats(
                    event.getId(),
                    request.seats()
            );

            throw exception;
        }
    }

    public List<ReservationResponse> getMyReservations() {

        String username = getCurrentUsername();

        AppUser customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found"
                ));

        return reservationRepository.findByCustomerId(customer.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReservationResponse> getAllReservations() {

        String username = getCurrentUsername();

        boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        List<Reservation> reservations =
                reservationRepository.findAll();

        if (isAdmin) {
            return reservations.stream()
                    .map(this::toResponse)
                    .toList();
        }

        return reservations.stream()
                .filter(reservation -> {
                    Event event = eventRepository
                            .findById(reservation.getEventId())
                            .orElse(null);

                    return event != null
                            && event.getOrganizer().equals(username);
                })
                .map(this::toResponse)
                .toList();
    }

    public void cancelReservation(String id) {

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Reservation not found"
                ));

        String username = getCurrentUsername();

        boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        AppUser customer = userRepository.findByUsername(username)
                .orElse(null);

        if (!isAdmin &&
                (customer == null ||
                        !reservation.getCustomerId().equals(customer.getId()))) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "You can only cancel your own reservations"
            );
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Reservation is already cancelled"
            );
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        eventService.increaseAvailableSeats(
                reservation.getEventId(),
                reservation.getSeats()
        );
    }

    private String getCurrentUsername() {

        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    private ReservationResponse toResponse(Reservation reservation) {

        return new ReservationResponse(
                reservation.getId(),
                reservation.getReservationCode(),
                reservation.getEventId(),
                reservation.getSeats(),
                reservation.getAmount(),
                reservation.getStatus()
        );
    }
}

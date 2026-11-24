
package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final MongoTemplate mongoTemplate;

    public EventService(
            EventRepository eventRepository,
            ReservationRepository reservationRepository,
            MongoTemplate mongoTemplate) {

        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public EventResponse createEvent(EventRequest request) {

        String username = getCurrentUsername();

        Event event = new Event();
        event.setName(request.name());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setEventDate(request.eventDate());
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(request.totalSeats());
        event.setTicketPrice(request.ticketPrice());
        event.setOrganizer(username);

        return toResponse(eventRepository.save(event));
    }

    public List<EventResponse> getAllEvents() {

        return eventRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EventResponse getEventById(String id) {

        Event event = findEvent(id);
        return toResponse(event);
    }

    public EventResponse updateEvent(String id, EventRequest request) {

        Event event = findEvent(id);

        checkOwnership(event);

        int soldSeats = event.getTotalSeats() - event.getAvailableSeats();

        if (request.totalSeats() < soldSeats) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Total seats cannot be less than seats already reserved"
            );
        }

        event.setName(request.name());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setEventDate(request.eventDate());
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(request.totalSeats() - soldSeats);
        event.setTicketPrice(request.ticketPrice());

        return toResponse(eventRepository.save(event));
    }

    public void deleteEvent(String id) {

        Event event = findEvent(id);

        if (reservationRepository.existsByEventId(id)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Cannot delete an event that has reservations"
            );
        }

        eventRepository.delete(event);
    }

    public void decreaseAvailableSeats(String eventId, int seats) {

        Query query = new Query(
                Criteria.where("_id").is(eventId)
                        .and("availableSeats").gte(seats)
        );

        Update update = new Update().inc("availableSeats", -seats);

        long modifiedCount = mongoTemplate
                .updateFirst(query, update, Event.class)
                .getModifiedCount();

        if (modifiedCount == 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Event not found or insufficient available seats"
            );
        }
    }

    public void increaseAvailableSeats(String eventId, int seats) {

        Query query = new Query(
                Criteria.where("_id").is(eventId)
        );

        Update update = new Update().inc("availableSeats", seats);

        mongoTemplate.updateFirst(query, update, Event.class);
    }

    private Event findEvent(String id) {

        return eventRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));
    }

    private void checkOwnership(Event event) {

        String username = getCurrentUsername();

        boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !event.getOrganizer().equals(username)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "You can only manage your own events"
            );
        }
    }

    private String getCurrentUsername() {

        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    private EventResponse toResponse(Event event) {

        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getVenue(),
                event.getEventDate(),
                event.getTotalSeats(),
                event.getAvailableSeats(),
                event.getTicketPrice(),
                event.getOrganizer()
        );
    }
}

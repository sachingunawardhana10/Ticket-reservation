
package com.example.ticketreservation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reservations")
public class Reservation {

    @Id
    private String id;

    private String reservationCode;

    private String customerId;

    private String eventId;

    private int seats;

    private BigDecimal amount;

    private ReservationStatus status;
}

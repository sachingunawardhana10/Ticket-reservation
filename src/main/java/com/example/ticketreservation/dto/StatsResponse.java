
package com.example.ticketreservation.dto;

import java.math.BigDecimal;

public record StatsResponse(

        long users,

        long events,

        long ticketsSold,

        BigDecimal revenue

) {}

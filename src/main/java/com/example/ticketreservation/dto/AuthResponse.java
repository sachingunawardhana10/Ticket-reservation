
package com.example.ticketreservation.dto;

public record AuthResponse(

        String token,

        String tokenType,

        long expiresInMs,

        String username,

        String fullName,

        com.example.ticketreservation.model.Role role

) {}

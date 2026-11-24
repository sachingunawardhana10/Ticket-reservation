
package com.example.ticketreservation.dto;

public record UserResponse(

        String id,

        String username,

        String fullName,

        com.example.ticketreservation.model.Role role

) {}

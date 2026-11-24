
package com.example.ticketreservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(

        @NotBlank
        String username,

        @NotBlank
        String password,

        @NotBlank
        String fullName,

        @NotNull
        String role

) {}

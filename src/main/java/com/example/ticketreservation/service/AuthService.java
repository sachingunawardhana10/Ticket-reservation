
package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.AuthResponse;
import com.example.ticketreservation.dto.LoginRequest;
import com.example.ticketreservation.dto.RegisterRequest;
import com.example.ticketreservation.dto.UserResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.UserRepository;
import com.example.ticketreservation.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {

        if (!Role.CUSTOMER.name().equalsIgnoreCase(request.role())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Public registration is only allowed for CUSTOMER accounts"
            );
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(Role.CUSTOMER);

        AppUser savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getFullName(),
                savedUser.getRole()
        );
    }

    public AuthResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username(),
                                request.password()
                        )
                );

        String username = authentication.getName();

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        String token = jwtService.generateToken(username);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );
    }

    public UserResponse getCurrentUser() {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );
    }

    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getRole()
                ))
                .toList();
    }
}

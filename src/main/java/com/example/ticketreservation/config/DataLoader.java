package com.example.ticketreservation.config;

import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        createUser("admin", "admin123", "System Administrator", Role.ADMIN);

        createUser("organizer", "organizer123", "Event Organizer", Role.ORGANIZER);
    }

    private void createUser(String username, String password,
                            String fullName, Role role) {

        if (!userRepository.existsByUsername(username)) {

            AppUser user = new AppUser();

            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setRole(role);

            userRepository.save(user);

            System.out.println("Created account: " + username);
        }
    }
}
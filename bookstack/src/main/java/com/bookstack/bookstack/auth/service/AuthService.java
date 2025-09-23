package com.bookstack.bookstack.auth.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookstack.bookstack.auth.model.User;
import com.bookstack.bookstack.auth.repository.UserRepository;
import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.ConflictException;
import com.bookstack.bookstack.common.exception.NotFoundException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final List<String> VALID_ROLES = Arrays.asList("ADMIN", "LIBRARIAN", "MEMBER");
    private static final String DEFAULT_ROLE = "MEMBER";

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(String username, String password, String role) {
        
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }
        
        String validatedRole = validateRole(role);
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(validatedRole);
        return userRepository.save(user);
    }

    public String login(String username, String password) {

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }
        return jwtService.generateToken(user);
    }

    private String validateRole(String role) {
        if (role == null || role.trim().isEmpty() || !VALID_ROLES.contains(role.toUpperCase())) {
            return DEFAULT_ROLE;
        }
        return role.toUpperCase(); // Normalize to uppercase
    }
}

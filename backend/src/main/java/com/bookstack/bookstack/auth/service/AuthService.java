package com.bookstack.bookstack.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.NotFoundException;
import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.service.UserService;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthService(PasswordEncoder passwordEncoder, JwtService jwtService, UserService userService) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    public User register(String username, String password) {
        return userService.createUser(username, password, "MEMBER");
    }

    public String login(String username, String password) {
        User user = userService.getUserByUsername(username)
            .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }
        return jwtService.generateToken(user);
    }
}
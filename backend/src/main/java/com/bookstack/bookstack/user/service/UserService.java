package com.bookstack.bookstack.user.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.ConflictException;
import com.bookstack.bookstack.common.exception.NotFoundException;
import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> VALID_ROLES = Arrays.asList("ADMIN", "LIBRARIAN", "MEMBER");
    private static final String DEFAULT_ROLE = "MEMBER";

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String username, String password, String role) {
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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> searchUsersByUsername(String username) {
        return userRepository.findByUsernameContainingIgnoreCase(username);
    }

    public List<User> getUsersByRole(String role) {
        String validatedRole = validateRole(role);
        return userRepository.findByRole(validatedRole);
    }

    public User updateUser(Long id, String username, String password, String role) {
        User user = getUserById(id);
        
        // Check if username is being changed and if new username is taken
        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }
        
        user.setUsername(username);
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setRole(validateRole(role));
        return userRepository.save(user);
    }

    public User updateUserRole(Long id, String role) {
        User user = getUserById(id);
        user.setRole(validateRole(role));
        return userRepository.save(user);
    }

    public User updateUserPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BadRequestException("Password cannot be empty");
        }
        
        User user = getUserById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public boolean userExists(Long id) {
        return userRepository.existsById(id);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    private String validateRole(String role) {
        if (role == null || role.trim().isEmpty() || !VALID_ROLES.contains(role.toUpperCase())) {
            return DEFAULT_ROLE;
        }
        return role.toUpperCase();
    }
}
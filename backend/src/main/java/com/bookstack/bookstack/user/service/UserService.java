package com.bookstack.bookstack.user.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.ConflictException;
import com.bookstack.bookstack.common.exception.NotFoundException;
import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.repository.UserRepository;

@Service
@Transactional
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
        // Check if username exists (including soft-deleted users)
        if (userRepository.existsByUsernameIncludingDeleted(username)) {
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
        return userRepository.findAllActive();
    }

    public List<User> getAllUsersIncludingDeleted() {
        return userRepository.findAll();
    }

    public List<User> getDeletedUsers() {
        return userRepository.findAllDeleted();
    }

    public User getUserById(Long id) {
        return userRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    public User getUserByIdIncludingDeleted(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findActiveByUsername(username);
    }

    public List<User> searchUsersByUsername(String username) {
        return userRepository.findActiveByUsernameContainingIgnoreCase(username);
    }

    public List<User> getUsersByRole(String role) {
        String validatedRole = validateRole(role);
        return userRepository.findActiveByRole(validatedRole);
    }

    public User updateUser(Long id, String username, String password, String role) {
        User user = getUserById(id);
        
        // Check if username is being changed and if new username is taken
        if (!user.getUsername().equals(username) && userRepository.existsByUsernameIncludingDeleted(username)) {
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

    // Soft delete
    public void deleteUser(Long id) {
        User user = getUserById(id);
        user.markAsDeleted();
        userRepository.save(user);
    }

    // Hard delete (physical deletion) - for admin use only
    public void hardDeleteUser(Long id) {
        if (!userRepository.existsByIdIncludingDeleted(id)) {
            throw new NotFoundException("User not found with id: " + id);
        }
        userRepository.hardDeleteById(id);
    }

    // Restore soft-deleted user
    public User restoreUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        
        if (!user.isDeleted()) {
            throw new BadRequestException("User is not deleted");
        }
        
        // Check if username is still unique among active users
        if (userRepository.existsActiveByUsername(user.getUsername())) {
            throw new ConflictException("Cannot restore user: username is already taken by another active user");
        }
        
        user.restore();
        return userRepository.save(user);
    }

    public boolean userExists(Long id) {
        return userRepository.existsById(id); // This will use the @Where clause
    }

    public boolean userExistsIncludingDeleted(Long id) {
        return userRepository.existsByIdIncludingDeleted(id);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsActiveByUsername(username);
    }

    public boolean usernameExistsIncludingDeleted(String username) {
        return userRepository.existsByUsernameIncludingDeleted(username);
    }

    private String validateRole(String role) {
        if (role == null || role.trim().isEmpty() || !VALID_ROLES.contains(role.toUpperCase())) {
            return DEFAULT_ROLE;
        }
        return role.toUpperCase();
    }
}
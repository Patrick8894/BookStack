package com.bookstack.bookstack.user.service;

import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.ConflictException;
import com.bookstack.bookstack.common.exception.NotFoundException;
import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User validUser;
    private User validUser2;
    private List<User> userList;

    @BeforeEach
    void setUp() {
        validUser = new User();
        validUser.setId(1L);
        validUser.setUsername("john_doe");
        validUser.setPassword("encoded_password");
        validUser.setRole("MEMBER");

        validUser2 = new User();
        validUser2.setId(2L);
        validUser2.setUsername("jane_admin");
        validUser2.setPassword("encoded_password2");
        validUser2.setRole("ADMIN");

        userList = Arrays.asList(validUser, validUser2);
    }

    // ===== CREATE USER TESTS =====

    @Test
    void createUser_WithValidData_ShouldCreateAndReturnUser() {
        // Given
        String username = "new_user";
        String password = "password123";
        String role = "LIBRARIAN";

        when(userRepository.existsByUsernameIncludingDeleted(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(validUser);

        // When
        User result = userService.createUser(username, password, role);

        // Then
        assertEquals(validUser, result);
        verify(userRepository).existsByUsernameIncludingDeleted(username);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithExistingUsername_ShouldThrowConflictException() {
        // Given
        String username = "existing_user";
        String password = "password123";
        String role = "MEMBER";

        when(userRepository.existsByUsernameIncludingDeleted(username)).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
            () -> userService.createUser(username, password, role));

        assertEquals("Username is already taken", exception.getMessage());
        verify(userRepository).existsByUsernameIncludingDeleted(username);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WithInvalidRole_ShouldUseDefaultRole() {
        // Given
        String username = "new_user";
        String password = "password123";
        String role = "INVALID_ROLE";

        when(userRepository.existsByUsernameIncludingDeleted(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals("MEMBER", user.getRole()); // Should default to MEMBER
            return user;
        });

        // When
        userService.createUser(username, password, role);

        // Then
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithNullRole_ShouldUseDefaultRole() {
        // Given
        String username = "new_user";
        String password = "password123";
        String role = null;

        when(userRepository.existsByUsernameIncludingDeleted(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals("MEMBER", user.getRole()); // Should default to MEMBER
            return user;
        });

        // When
        userService.createUser(username, password, role);

        // Then
        verify(userRepository).save(any(User.class));
    }

    // ===== GET ALL USERS TESTS =====

    @Test
    void getAllUsers_ShouldReturnActiveUsers() {
        // Given
        when(userRepository.findAllActive()).thenReturn(userList);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertEquals(2, result.size());
        assertEquals(userList, result);
        verify(userRepository).findAllActive();
    }

    @Test
    void getAllUsersIncludingDeleted_ShouldReturnAllUsers() {
        // Given
        when(userRepository.findAll()).thenReturn(userList);

        // When
        List<User> result = userService.getAllUsersIncludingDeleted();

        // Then
        assertEquals(2, result.size());
        assertEquals(userList, result);
        verify(userRepository).findAll();
    }

    @Test
    void getDeletedUsers_ShouldReturnDeletedUsers() {
        // Given
        when(userRepository.findAllDeleted()).thenReturn(Collections.singletonList(validUser));

        // When
        List<User> result = userService.getDeletedUsers();

        // Then
        assertEquals(1, result.size());
        assertEquals(validUser, result.get(0));
        verify(userRepository).findAllDeleted();
    }

    // ===== GET USER BY ID TESTS =====

    @Test
    void getUserById_WithValidId_ShouldReturnUser() {
        // Given
        Long userId = 1L;
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(validUser));

        // When
        User result = userService.getUserById(userId);

        // Then
        assertEquals(validUser, result);
        verify(userRepository).findActiveById(userId);
    }

    @Test
    void getUserById_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        Long userId = 999L;
        when(userRepository.findActiveById(userId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> userService.getUserById(userId));

        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).findActiveById(userId);
    }

    @Test
    void getUserByIdIncludingDeleted_WithValidId_ShouldReturnUser() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));

        // When
        User result = userService.getUserByIdIncludingDeleted(userId);

        // Then
        assertEquals(validUser, result);
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserByIdIncludingDeleted_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> userService.getUserByIdIncludingDeleted(userId));

        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).findById(userId);
    }

    // ===== GET USER BY USERNAME TESTS =====

    @Test
    void getUserByUsername_WithValidUsername_ShouldReturnUser() {
        // Given
        String username = "john_doe";
        when(userRepository.findActiveByUsername(username)).thenReturn(Optional.of(validUser));

        // When
        Optional<User> result = userService.getUserByUsername(username);

        // Then
        assertTrue(result.isPresent());
        assertEquals(validUser, result.get());
        verify(userRepository).findActiveByUsername(username);
    }

    @Test
    void getUserByUsername_WithInvalidUsername_ShouldReturnEmpty() {
        // Given
        String username = "nonexistent";
        when(userRepository.findActiveByUsername(username)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserByUsername(username);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findActiveByUsername(username);
    }

    // ===== SEARCH TESTS =====

    @Test
    void searchUsersByUsername_ShouldReturnMatchingUsers() {
        // Given
        String username = "john";
        when(userRepository.findActiveByUsernameContainingIgnoreCase(username))
            .thenReturn(Collections.singletonList(validUser));

        // When
        List<User> result = userService.searchUsersByUsername(username);

        // Then
        assertEquals(1, result.size());
        assertEquals(validUser, result.get(0));
        verify(userRepository).findActiveByUsernameContainingIgnoreCase(username);
    }

    @Test
    void getUsersByRole_WithValidRole_ShouldReturnUsers() {
        // Given
        String role = "ADMIN";
        when(userRepository.findActiveByRole(role)).thenReturn(Collections.singletonList(validUser2));

        // When
        List<User> result = userService.getUsersByRole(role);

        // Then
        assertEquals(1, result.size());
        assertEquals(validUser2, result.get(0));
        verify(userRepository).findActiveByRole(role);
    }

    @Test
    void getUsersByRole_WithInvalidRole_ShouldUseDefaultRole() {
        // Given
        String role = "INVALID_ROLE";
        when(userRepository.findActiveByRole("MEMBER")).thenReturn(Collections.singletonList(validUser));

        // When
        List<User> result = userService.getUsersByRole(role);

        // Then
        assertEquals(1, result.size());
        verify(userRepository).findActiveByRole("MEMBER"); // Should use default role
    }

    // ===== UPDATE USER TESTS =====

    @Test
    void updateUser_WithValidData_ShouldUpdateAndReturnUser() {
        // Given
        Long userId = 1L;
        String newUsername = "updated_username";
        String newPassword = "new_password";
        String newRole = "LIBRARIAN";

        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(validUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");
        when(userRepository.save(validUser)).thenReturn(validUser);

        // When
        User result = userService.updateUser(userId, newUsername, newPassword, newRole);

        // Then
        assertEquals(validUser, result);
        verify(userRepository).findActiveById(userId);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(validUser);

        // Verify the user was updated
        assertEquals(newUsername, validUser.getUsername());
        assertEquals("LIBRARIAN", validUser.getRole());
    }

    @Test
    void updateUser_WithNewUsernameThatExists_ShouldThrowConflictException() {
        // Given
        Long userId = 1L;
        String newUsername = "existing_username";
        String newPassword = "new_password";
        String newRole = "LIBRARIAN";

        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(validUser));
        when(userRepository.existsByUsernameIncludingDeleted(newUsername)).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
            () -> userService.updateUser(userId, newUsername, newPassword, newRole));

        assertEquals("Username is already taken", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_WithEmptyPassword_ShouldNotUpdatePassword() {
        // Given
        Long userId = 1L;
        String newUsername = "updated_username";
        String newPassword = ""; // Empty password
        String newRole = "LIBRARIAN";
        String originalPassword = validUser.getPassword();

        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(validUser));
        when(userRepository.save(validUser)).thenReturn(validUser);

        // When
        User result = userService.updateUser(userId, newUsername, newPassword, newRole);

        // Then
        assertEquals(validUser, result);
        verify(passwordEncoder, never()).encode(anyString()); // Password should not be encoded
        assertEquals(originalPassword, validUser.getPassword()); // Password unchanged
    }

    // ===== UPDATE USER ROLE TESTS =====

    @Test
    void updateUserRole_WithValidRole_ShouldUpdateRole() {
        // Given
        Long userId = 1L;
        String newRole = "ADMIN";

        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(validUser));
        when(userRepository.save(validUser)).thenReturn(validUser);

        // When
        User result = userService.updateUserRole(userId, newRole);

        // Then
        assertEquals(validUser, result);
        assertEquals("ADMIN", validUser.getRole());
        verify(userRepository).findActiveById(userId);
        verify(userRepository).save(validUser);
    }

    @Test
    void updateUserRole_WithInvalidRole_ShouldUseDefaultRole() {
        // Given
        Long userId = 1L;
        String newRole = "INVALID_ROLE";

        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(validUser));
        when(userRepository.save(validUser)).thenReturn(validUser);

        // When
        User result = userService.updateUserRole(userId, newRole);

        // Then
        assertEquals(validUser, result);
        assertEquals("MEMBER", validUser.getRole()); // Should default to MEMBER
        verify(userRepository).save(validUser);
    }

    // ===== UPDATE USER PASSWORD TESTS =====

    @Test
    void updateUserPassword_WithValidPassword_ShouldUpdatePassword() {
        // Given
        Long userId = 1L;
        String newPassword = "new_password123";

        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(validUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded_new_password");
        when(userRepository.save(validUser)).thenReturn(validUser);

        // When
        User result = userService.updateUserPassword(userId, newPassword);

        // Then
        assertEquals(validUser, result);
        verify(userRepository).findActiveById(userId);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(validUser);
    }

    @Test
    void updateUserPassword_WithEmptyPassword_ShouldThrowBadRequestException() {
        // Given
        Long userId = 1L;
        String newPassword = "";

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> userService.updateUserPassword(userId, newPassword));

        assertEquals("Password cannot be empty", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserPassword_WithNullPassword_ShouldThrowBadRequestException() {
        // Given
        Long userId = 1L;
        String newPassword = null;

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> userService.updateUserPassword(userId, newPassword));

        assertEquals("Password cannot be empty", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    // ===== DELETE USER TESTS =====

    @Test
    void deleteUser_WithValidId_ShouldMarkAsDeleted() {
        // Given
        Long userId = 1L;
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(validUser));
        when(userRepository.save(validUser)).thenReturn(validUser);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).findActiveById(userId);
        verify(userRepository).save(validUser);
    }

    @Test
    void hardDeleteUser_WithValidId_ShouldDeletePermanently() {
        // Given
        Long userId = 1L;
        when(userRepository.existsByIdIncludingDeleted(userId)).thenReturn(true);
        doNothing().when(userRepository).hardDeleteById(userId);

        // When
        userService.hardDeleteUser(userId);

        // Then
        verify(userRepository).existsByIdIncludingDeleted(userId);
        verify(userRepository).hardDeleteById(userId);
    }

    @Test
    void hardDeleteUser_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        Long userId = 999L;
        when(userRepository.existsByIdIncludingDeleted(userId)).thenReturn(false);

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> userService.hardDeleteUser(userId));

        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository, never()).hardDeleteById(any());
    }

    // ===== RESTORE USER TESTS =====

    @Test
    void restoreUser_WithValidDeletedUser_ShouldRestoreUser() {
        // Given
        Long userId = 1L;
        User deletedUser = new User();
        deletedUser.setId(userId);
        deletedUser.setUsername("deleted_user");
        deletedUser.markAsDeleted(); // Assume this method exists

        when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));
        when(userRepository.existsActiveByUsername(deletedUser.getUsername())).thenReturn(false);
        when(userRepository.save(deletedUser)).thenReturn(deletedUser);

        // When
        User result = userService.restoreUser(userId);

        // Then
        assertEquals(deletedUser, result);
        verify(userRepository).findById(userId);
        verify(userRepository).existsActiveByUsername(deletedUser.getUsername());
        verify(userRepository).save(deletedUser);
    }

    @Test
    void restoreUser_WithActiveUser_ShouldThrowBadRequestException() {
        // Given
        Long userId = 1L;
        User activeUser = new User();
        activeUser.setId(userId);
        activeUser.setUsername("active_user");
        // User is not deleted

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> userService.restoreUser(userId));

        assertEquals("User is not deleted", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void restoreUser_WithUsernameTaken_ShouldThrowConflictException() {
        // Given
        Long userId = 1L;
        User deletedUser = new User();
        deletedUser.setId(userId);
        deletedUser.setUsername("taken_username");
        deletedUser.markAsDeleted();

        when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));
        when(userRepository.existsActiveByUsername(deletedUser.getUsername())).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
            () -> userService.restoreUser(userId));

        assertEquals("Cannot restore user: username is already taken by another active user", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    // ===== EXISTENCE CHECK TESTS =====

    @Test
    void userExists_ShouldReturnTrue() {
        // Given
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        // When
        boolean result = userService.userExists(userId);

        // Then
        assertTrue(result);
        verify(userRepository).existsById(userId);
    }

    @Test
    void userExistsIncludingDeleted_ShouldReturnTrue() {
        // Given
        Long userId = 1L;
        when(userRepository.existsByIdIncludingDeleted(userId)).thenReturn(true);

        // When
        boolean result = userService.userExistsIncludingDeleted(userId);

        // Then
        assertTrue(result);
        verify(userRepository).existsByIdIncludingDeleted(userId);
    }

    @Test
    void usernameExists_ShouldReturnTrue() {
        // Given
        String username = "john_doe";
        when(userRepository.existsActiveByUsername(username)).thenReturn(true);

        // When
        boolean result = userService.usernameExists(username);

        // Then
        assertTrue(result);
        verify(userRepository).existsActiveByUsername(username);
    }

    @Test
    void usernameExistsIncludingDeleted_ShouldReturnTrue() {
        // Given
        String username = "john_doe";
        when(userRepository.existsByUsernameIncludingDeleted(username)).thenReturn(true);

        // When
        boolean result = userService.usernameExistsIncludingDeleted(username);

        // Then
        assertTrue(result);
        verify(userRepository).existsByUsernameIncludingDeleted(username);
    }
}
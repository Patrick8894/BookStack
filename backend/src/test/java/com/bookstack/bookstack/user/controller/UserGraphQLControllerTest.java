package com.bookstack.bookstack.user.controller;

import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@GraphQlTest(controllers = UserGraphQLController.class)
@TestPropertySource(properties = "bookstack.auth.enabled=false") // Disable auth aspect
class UserGraphQLControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private UserService userService;

    private User sampleUser;
    private List<User> sampleUsers;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("john_doe");
        sampleUser.setRole("MEMBER");

        sampleUsers = Arrays.asList(sampleUser);
    }

    @Test
    void allUsers_ShouldReturnExpectedShape() {
        // Given
        when(userService.getAllUsers()).thenReturn(sampleUsers);

        // When & Then - Verify field names and structure
        graphQlTester.document("""
            query {
                allUsers {
                    id
                    username
                    role
                }
            }
            """)
            .execute()
            .path("allUsers[0].id").entity(Long.class).isEqualTo(1L)
            .path("allUsers[0].username").entity(String.class).isEqualTo("john_doe")
            .path("allUsers[0].role").entity(String.class).isEqualTo("MEMBER");
    }

    @Test
    void userById_WithNullResult_ShouldReturnNull() {
        // Given
        when(userService.getUserById(999L)).thenReturn(null);

        // When & Then - Verify null handling
        graphQlTester.document("""
            query {
                userById(id: 999) {
                    id
                    username
                }
            }
            """)
            .execute()
            .path("userById").valueIsNull();
    }

    @Test
    void addUser_WithValidInput_ShouldReturnUser() {
        // Given
        User newUser = new User();
        newUser.setId(2L);
        newUser.setUsername("new_user");
        newUser.setRole("LIBRARIAN");

        when(userService.createUser(anyString(), anyString(), anyString())).thenReturn(newUser);

        // When & Then - Test successful mutation
        graphQlTester.document("""
            mutation {
                addUser(input: {
                    username: "new_user",
                    password: "password123",
                    role: "LIBRARIAN"
                }) {
                    id
                    username
                    role
                }
            }
            """)
            .execute()
            .path("addUser.id").entity(Long.class).isEqualTo(2L)
            .path("addUser.username").entity(String.class).isEqualTo("new_user")
            .path("addUser.role").entity(String.class).isEqualTo("LIBRARIAN");
    }

    @Test
    void usersByRole_ShouldReturnExpectedShape() {
        // Given
        when(userService.getUsersByRole("MEMBER")).thenReturn(sampleUsers);

        // When & Then - Verify query with parameters
        graphQlTester.document("""
            query {
                usersByRole(role: "MEMBER") {
                    id
                    username
                    role
                }
            }
            """)
            .execute()
            .path("usersByRole").entityList(User.class).hasSize(1)
            .path("usersByRole[0].id").entity(Long.class).isEqualTo(1L)
            .path("usersByRole[0].username").entity(String.class).isEqualTo("john_doe")
            .path("usersByRole[0].role").entity(String.class).isEqualTo("MEMBER");
    }
}
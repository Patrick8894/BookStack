package com.bookstack.bookstack.auth.controller;

import com.bookstack.bookstack.auth.dto.LoginResponse;
import com.bookstack.bookstack.auth.service.AuthService;
import com.bookstack.bookstack.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private LoginResponse sampleLoginResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

        // Create a sample user
        User sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("john_doe");
        sampleUser.setRole("MEMBER");

        // Create LoginResponse with nested UserDto
        LoginResponse.UserDto userDto = new LoginResponse.UserDto(sampleUser);
        sampleLoginResponse = new LoginResponse("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", userDto);
    }

    @Test
    void register_WithValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        doNothing().when(authService).register(anyString(), anyString());
        
        String requestBody = """
            {
                "username": "new_user",
                "password": "password123"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnLoginResponse() throws Exception {
        // Given
        when(authService.login(anyString(), anyString())).thenReturn(sampleLoginResponse);

        String requestBody = """
            {
                "username": "john_doe",
                "password": "password123"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
                .andExpect(jsonPath("$.user.id").value(1L))
                .andExpect(jsonPath("$.user.username").value("john_doe"))
                .andExpect(jsonPath("$.user.role").value("MEMBER"));
    }

    @Test
    void register_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // Given - malformed JSON
        String requestBody = """
            {
                "username": "new_user"
                // missing comma and password field
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // Given - malformed JSON
        String requestBody = """
            {
                "username": "john_doe"
                // missing comma and password field
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
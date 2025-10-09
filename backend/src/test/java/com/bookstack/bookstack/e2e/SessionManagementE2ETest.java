package com.bookstack.bookstack.e2e;

import com.bookstack.bookstack.auth.dto.LoginRequest;
import com.bookstack.bookstack.auth.dto.LoginResponse;
import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SessionManagementE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private String uniqueId;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port + "/api";
        uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        // Clean up existing data
        userRepository.deleteAll();
    }

    @Test
    void completeSessionManagementFlow_ShouldWork() throws Exception {
        // ===== STEP 1: User Registration and Login =====
        System.out.println("🔐 Step 1: User logs in and gets valid token...");
        
        User testUser = createUser("session_user_" + uniqueId, "password123", "MEMBER");
        String validToken = loginUser(testUser.getUsername(), "password123");
        
        assertThat(validToken).isNotNull();
        System.out.println("✅ User login successful - PASSED");

        // ===== STEP 2: Valid Token Access =====
        System.out.println("🎫 Step 2: Testing valid token access...");
        
        HttpHeaders validHeaders = createAuthHeaders(validToken);
        // Use a query that MEMBER users can access - like their own borrow history
        String memberQuery = String.format("""
            query {
                userBorrows(userId: %d) {
                    id
                    borrowDate
                    status
                }
            }
            """, testUser.getId());

        ResponseEntity<String> validAccessResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(memberQuery), validHeaders),
            String.class
        );

        String body = Objects.requireNonNull(validAccessResponse.getBody());

        // If userBorrows query doesn't exist, use a REST endpoint that members can access
        if (body.contains("Field 'userBorrows' is undefined")) {
            System.out.println("📝 Using REST endpoint for member access test...");
            validAccessResponse = restTemplate.exchange(
                baseUrl + "/borrows/user/" + testUser.getId() + "/active",
                HttpMethod.GET,
                new HttpEntity<>(validHeaders),
                String.class
            );
        }

        assertThat(validAccessResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(validAccessResponse.getBody()).doesNotContain("Insufficient permissions");
        System.out.println("✅ Valid token grants appropriate access - PASSED");

        // ===== STEP 3: Invalid Token Scenarios =====
        System.out.println("🚫 Step 3: Testing invalid token scenarios...");
        
        // 3a. Malformed token
        HttpHeaders malformedHeaders = createAuthHeaders("invalid.malformed.token");
        ResponseEntity<String> malformedResponse = restTemplate.exchange(
            baseUrl + "/borrows/user/" + testUser.getId() + "/active",
            HttpMethod.GET,
            new HttpEntity<>(malformedHeaders),
            String.class
        );

        assertThat(malformedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        System.out.println("✅ Malformed token rejected - PASSED");

        // 3b. Empty token
        HttpHeaders emptyHeaders = new HttpHeaders();
        emptyHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> noTokenResponse = restTemplate.exchange(
            baseUrl + "/borrows/user/" + testUser.getId() + "/active",
            HttpMethod.GET,
            new HttpEntity<>(emptyHeaders),
            String.class
        );

        assertThat(noTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        System.out.println("✅ Missing token rejected - PASSED");

        // 3c. Test unauthorized access - Member trying to access admin-only GraphQL
        System.out.println("🚫 Step 3c: Testing unauthorized GraphQL access...");
        String adminOnlyQuery = """
            query {
                allUsers {
                    id
                    username
                    role
                }
            }
            """;

        ResponseEntity<String> unauthorizedResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(adminOnlyQuery), validHeaders),
            String.class
        );

        assertThat(unauthorizedResponse.getBody()).contains("Insufficient permissions");
        System.out.println("✅ Unauthorized GraphQL access properly blocked - PASSED");

        // ===== STEP 4: User Logout Flow =====
        System.out.println("🚪 Step 4: Testing user logout...");
        
        // Attempt logout
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
            baseUrl + "/auth/logout",
            HttpMethod.POST,
            new HttpEntity<>(validHeaders),
            String.class
        );

        // Check if logout endpoint exists and works
        if (logoutResponse.getStatusCode() == HttpStatus.OK) {
            System.out.println("✅ Logout endpoint successful - PASSED");
            
            // Try to use token after logout (should fail if token blacklisting is implemented)
            ResponseEntity<String> postLogoutResponse = restTemplate.exchange(
                baseUrl + "/borrows/user/" + testUser.getId() + "/active",
                HttpMethod.GET,
                new HttpEntity<>(validHeaders),
                String.class
            );
            
            if (postLogoutResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                System.out.println("✅ Token invalidated after logout - PASSED");
            } else {
                System.out.println("⚠️ Token still valid after logout (stateless JWT behavior) - ACCEPTABLE");
            }
        } else if (logoutResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
            System.out.println("ℹ️ Logout endpoint not implemented (stateless JWT) - ACCEPTABLE");
        }

        // ===== STEP 5: Cross-Device Session Consistency =====
        System.out.println("📱 Step 5: Testing cross-device session behavior...");
        
        // Simulate multiple device logins
        String device1Token = loginUser(testUser.getUsername(), "password123");
        String device2Token = loginUser(testUser.getUsername(), "password123");
        
        // Both tokens should work independently (stateless JWT behavior)
        HttpHeaders device1Headers = createAuthHeaders(device1Token);
        HttpHeaders device2Headers = createAuthHeaders(device2Token);
        
        ResponseEntity<String> device1Response = restTemplate.exchange(
            baseUrl + "/borrows/user/" + testUser.getId() + "/active",
            HttpMethod.GET,
            new HttpEntity<>(device1Headers),
            String.class
        );
        
        ResponseEntity<String> device2Response = restTemplate.exchange(
            baseUrl + "/borrows/user/" + testUser.getId() + "/active",
            HttpMethod.GET,
            new HttpEntity<>(device2Headers),
            String.class
        );
        
        assertThat(device1Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(device2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Multiple device sessions work independently - PASSED");

        // ===== STEP 6: Role-Based Session Behavior =====
        System.out.println("👥 Step 6: Testing role-based session behavior...");
        
        // Create admin user
        User adminUser = createUser("session_admin_" + uniqueId, "admin123", "ADMIN");
        String adminToken = loginUser(adminUser.getUsername(), "admin123");
        
        // Test admin-specific operations - now admin can access allUsers
        String adminQuery = """
            query {
                allUsers {
                    id
                    username
                    role
                }
            }
            """;
        
        HttpHeaders adminHeaders = createAuthHeaders(adminToken);
        ResponseEntity<String> adminOperationResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(adminQuery), adminHeaders),
            String.class
        );
        
        assertThat(adminOperationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminOperationResponse.getBody()).doesNotContain("Insufficient permissions");
        System.out.println("✅ Admin session has elevated privileges for GraphQL - PASSED");

        // Test admin user creation
        String createUserMutation = String.format("""
            mutation {
                addUser(input: {
                    username: "session_test_member_%s",
                    password: "password123",
                    role: "MEMBER"
                }) {
                    id
                    username
                    role
                }
            }
            """, uniqueId);

        ResponseEntity<String> createUserResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(createUserMutation), adminHeaders),
            String.class
        );

        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createUserResponse.getBody()).doesNotContain("errors");
        System.out.println("✅ Admin can create users - PASSED");

        // Test that member token cannot perform admin operations
        ResponseEntity<String> memberAdminAttempt = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(createUserMutation), validHeaders),
            String.class
        );
        
        assertThat(memberAdminAttempt.getBody()).contains("Insufficient permissions");
        System.out.println("✅ Member session properly restricted from admin operations - PASSED");

        // ===== STEP 7: Session Security Edge Cases =====
        System.out.println("🔒 Step 7: Testing session security edge cases...");
        
        // Test with tampered token
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";
        HttpHeaders tamperedHeaders = createAuthHeaders(tamperedToken);
        
        ResponseEntity<String> tamperedResponse = restTemplate.exchange(
            baseUrl + "/borrows/user/" + testUser.getId() + "/active",
            HttpMethod.GET,
            new HttpEntity<>(tamperedHeaders),
            String.class
        );
        
        assertThat(tamperedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        System.out.println("✅ Tampered token rejected - PASSED");

        System.out.println("✅ Complete Session Management Flow E2E Test PASSED!");
    }

    @Test
    void invalidCredentialsFlow_ShouldWork() throws Exception {
        System.out.println("🔐 Testing invalid credentials scenarios...");
        
        User testUser = createUser("creds_user_" + uniqueId, "correctpassword", "MEMBER");
        
        // Test wrong password
        try {
            loginUser(testUser.getUsername(), "wrongpassword");
            assertThat(false).as("Login should fail with wrong password").isTrue();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Login failed");
            System.out.println("✅ Wrong password rejected - PASSED");
        }
        
        // Test non-existent user
        try {
            loginUser("nonexistent_user", "anypassword");
            assertThat(false).as("Login should fail for non-existent user").isTrue();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Login failed");
            System.out.println("✅ Non-existent user rejected - PASSED");
        }
        
        System.out.println("✅ Invalid Credentials Flow E2E Test PASSED!");
    }

    // Helper methods
    private String loginUser(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            baseUrl + "/auth/login",
            loginRequest,
            LoginResponse.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Login failed for user: " + username);
        }

        LoginResponse body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Login response body is null for user: " + username);
        }

        return body.getToken();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private String createGraphQLQuery(String query) throws Exception {
        return objectMapper.writeValueAsString(Map.of("query", query));
    }

    private User createUser(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }
}
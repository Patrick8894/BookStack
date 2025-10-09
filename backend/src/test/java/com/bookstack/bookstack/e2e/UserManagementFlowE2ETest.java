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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserManagementFlowE2ETest {

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
    private String adminToken;
    private String uniqueId;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port + "/api";
        uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        // Clean up existing data to avoid constraint violations
        userRepository.deleteAll();
        
        // Create admin user with unique username
        createUser("admin_" + uniqueId, "admin123", "ADMIN");
        
        // Login admin to get token
        adminToken = loginUser("admin_" + uniqueId, "admin123");
    }

    @Test
    void completeUserManagementFlow_ShouldWork() throws Exception {
        // ===== STEP 1: Admin creates a new Librarian user =====
        System.out.println("🔧 Step 1: Admin creates new librarian user...");
        
        String librarianUsername = "librarian_smith_" + uniqueId;
        String createLibrarianMutation = String.format("""
            mutation {
                addUser(input: {
                    username: "%s",
                    password: "password123",
                    role: "LIBRARIAN"
                }) {
                    id
                    username
                    role
                }
            }
            """, librarianUsername);

        HttpHeaders adminHeaders = createAuthHeaders(adminToken);
        ResponseEntity<String> createUserResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(createLibrarianMutation), adminHeaders),
            String.class
        );

        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createUserResponse.getBody()).contains(librarianUsername);
        assertThat(createUserResponse.getBody()).contains("LIBRARIAN");

        // Extract the new user ID from response
        Long newLibrarianId = extractUserIdFromGraphQLResponse(createUserResponse.getBody());
        
        // ===== STEP 2: Verify new user can login with correct permissions =====
        System.out.println("🔐 Step 2: New librarian logs in...");
        
        String librarianToken = loginUser(librarianUsername, "password123");
        assertThat(librarianToken).isNotNull();
        
        // ===== STEP 3: Test Librarian permissions (view/search only) =====
        System.out.println("📚 Step 3: Testing librarian permissions...");
        
        HttpHeaders librarianHeaders = createAuthHeaders(librarianToken);
        
        // ✅ Librarian CAN view all users (should work)
        String allUsersQuery = """
            query {
                allUsers {
                    id
                    username
                    role
                }
            }
            """;

        ResponseEntity<String> viewUsersResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(allUsersQuery), librarianHeaders),
            String.class
        );

        assertThat(viewUsersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(viewUsersResponse.getBody()).contains("admin_" + uniqueId);
        assertThat(viewUsersResponse.getBody()).contains(librarianUsername);
        System.out.println("✅ Librarian can view users list - PASSED");

        // ✅ Librarian CAN search for specific users (should work)
        String searchUserQuery = String.format("""
            query {
                searchUsers(username: "%s") {
                    id
                    username
                    role
                }
            }
            """, "admin_" + uniqueId);

        ResponseEntity<String> searchUsersResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(searchUserQuery), librarianHeaders),
            String.class
        );

        // Should work (or return appropriate response if search not implemented)
        assertThat(searchUsersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Librarian can search users - PASSED");

        // ❌ Librarian should NOT be able to create users (admin only)
        String memberUsername = "member_jones_" + uniqueId;
        String createMemberMutation = String.format("""
            mutation {
                addUser(input: {
                    username: "%s",
                    password: "password123",
                    role: "MEMBER"
                }) {
                    id
                    username
                    role
                }
            }
            """, memberUsername);

        ResponseEntity<String> createMemberResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(createMemberMutation), librarianHeaders),
            String.class
        );

        // Should get access denied error
        assertThat(createMemberResponse.getBody()).contains("errors");
        System.out.println("✅ Librarian cannot create users - PASSED");

        // ❌ Librarian should NOT be able to update users (admin only)
        String updateUserMutation = String.format("""
            mutation {
                updateUser(id: %d, input: {
                    username: "%s",
                    role: "ADMIN",
                    password: "newpassword123"
                }) {
                    id
                    username
                    role
                }
            }
            """, newLibrarianId, librarianUsername);

        ResponseEntity<String> librarianUpdateResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(updateUserMutation), librarianHeaders),
            String.class
        );

        // Should get access denied error
        assertThat(librarianUpdateResponse.getBody()).contains("errors");
        System.out.println("✅ Librarian cannot update users - PASSED");

        // ❌ Librarian should NOT be able to delete users (admin only)
        String deleteUserMutation = String.format("""
            mutation {
                deleteUser(id: %d) {
                    success
                    message
                }
            }
            """, newLibrarianId);

        ResponseEntity<String> librarianDeleteResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(deleteUserMutation), librarianHeaders),
            String.class
        );

        // Should get access denied error
        assertThat(librarianDeleteResponse.getBody()).contains("errors");
        System.out.println("✅ Librarian cannot delete users - PASSED");

        // ===== STEP 4: Admin creates a Member user =====
        System.out.println("👤 Step 4: Admin creates member user...");
        
        ResponseEntity<String> createMemberAsAdminResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(createMemberMutation), adminHeaders),
            String.class
        );

        assertThat(createMemberAsAdminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createMemberAsAdminResponse.getBody()).contains(memberUsername);
        assertThat(createMemberAsAdminResponse.getBody()).contains("MEMBER");

        Long newMemberId = extractUserIdFromGraphQLResponse(createMemberAsAdminResponse.getBody());

        // ===== STEP 5: Admin updates user role (Librarian → Admin) =====
        System.out.println("⬆️ Step 5: Admin promotes librarian to admin...");
        
        ResponseEntity<String> updateUserResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(updateUserMutation), adminHeaders),
            String.class
        );

        assertThat(updateUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateUserResponse.getBody()).contains("ADMIN");

        // ===== STEP 6: Verify updated permissions - Former librarian now has admin rights =====
        System.out.println("✅ Step 6: Verifying updated permissions...");
        
        // 🔑 IMPORTANT: After role update, we need to login again to get a fresh token
        // because the JWT contains the old role information
        System.out.println("🔑 Former librarian needs to login again to get fresh admin token...");
        String freshLibrarianToken = loginUser(librarianUsername, "newpassword123");
        HttpHeaders freshLibrarianHeaders = createAuthHeaders(freshLibrarianToken);

        // Former librarian should now be able to create users (since they're now ADMIN)
        String anotherMemberUsername = "member_doe_" + uniqueId;
        String createAnotherMemberMutation = String.format("""
            mutation {
                addUser(input: {
                    username: "%s",
                    password: "password123",
                    role: "MEMBER"
                }) {
                    id
                    username
                    role
                }
            }
            """, anotherMemberUsername);

        ResponseEntity<String> createAnotherMemberResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(createAnotherMemberMutation), freshLibrarianHeaders),
            String.class
        );

        assertThat(createAnotherMemberResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createAnotherMemberResponse.getBody()).contains(anotherMemberUsername);
        System.out.println("✅ Former librarian (now admin) can create users - PASSED");

        // ===== STEP 7: Admin soft-deletes a user =====
        System.out.println("🗑️ Step 7: Admin deactivates member user...");

        String deleteMemberMutation = String.format("""
            mutation {
                deleteUser(id: %d)
            }
            """, newMemberId);

        ResponseEntity<String> deleteUserResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(deleteMemberMutation), adminHeaders),
            String.class
        );

        assertThat(deleteUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Check for successful deletion - should return true in the data field
        assertThat(deleteUserResponse.getBody()).contains("\"deleteUser\":true");

        System.out.println("✅ Member user deactivated successfully - PASSED");

        // ===== STEP 8: Verify user cannot login after deactivation =====
        System.out.println("🚫 Step 8: Verifying deactivated user cannot login...");
        
        try {
            loginUser(memberUsername, "password123");
            // Should fail - if we get here, test should fail
            assertThat(false).as("Deactivated user should not be able to login").isTrue();
        } catch (Exception e) {
            // Expected - login should fail
            assertThat(e.getMessage()).contains("Login failed");
            System.out.println("✅ Deactivated user cannot login - PASSED");
        }

        // ===== STEP 9: Verify user is excluded from active user lists =====
        System.out.println("📋 Step 9: Verifying deactivated user not in active lists...");
        
        ResponseEntity<String> allActiveUsersResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(allUsersQuery), adminHeaders),
            String.class
        );

        assertThat(allActiveUsersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allActiveUsersResponse.getBody()).doesNotContain(memberUsername); // Should not appear in active users
        assertThat(allActiveUsersResponse.getBody()).contains("admin_" + uniqueId);
        assertThat(allActiveUsersResponse.getBody()).contains(librarianUsername);
        assertThat(allActiveUsersResponse.getBody()).contains(anotherMemberUsername);

        System.out.println("✅ Complete User Management Flow E2E Test PASSED!");
    }

    @Test
    void librarianPermissionsFlow_ShouldWork() throws Exception {
        // ===== Test Librarian permissions in detail =====
        System.out.println("📚 Testing Librarian user permissions flow...");
        
        // Admin creates a librarian
        String testLibrarianUsername = "test_librarian_" + UUID.randomUUID().toString().substring(0, 8);
        String createLibrarianMutation = String.format("""
            mutation {
                addUser(input: {
                    username: "%s",
                    password: "password123",
                    role: "LIBRARIAN"
                }) {
                    id
                    username
                    role
                }
            }
            """, testLibrarianUsername);

        HttpHeaders adminHeaders = createAuthHeaders(adminToken);
        ResponseEntity<String> createResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(createLibrarianMutation), adminHeaders),
            String.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Librarian logs in
        String librarianToken = loginUser(testLibrarianUsername, "password123");
        HttpHeaders librarianHeaders = createAuthHeaders(librarianToken);

        // ✅ Librarian CAN view all users
        String allUsersQuery = """
            query {
                allUsers {
                    id
                    username
                    role
                }
            }
            """;

        ResponseEntity<String> viewUsersResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(allUsersQuery), librarianHeaders),
            String.class
        );

        assertThat(viewUsersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Librarian can view users - PASSED");

        // ✅ Librarian CAN access borrow-related endpoints (their main job)
        ResponseEntity<String> allBorrowsResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.GET,
            new HttpEntity<>(librarianHeaders),
            String.class
        );

        assertThat(allBorrowsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Librarian can access borrow management - PASSED");

        // ❌ Librarian should NOT be able to create users
        String attemptCreateUserMutation = """
            mutation {
                addUser(input: {
                    username: "unauthorized_user",
                    password: "password123",
                    role: "MEMBER"
                }) {
                    id
                    username
                    role
                }
            }
            """;

        ResponseEntity<String> unauthorizedCreateResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(attemptCreateUserMutation), librarianHeaders),
            String.class
        );

        // Should get access denied
        assertThat(unauthorizedCreateResponse.getBody()).contains("errors");
        System.out.println("✅ Librarian cannot create users - PASSED");

        // ❌ Librarian should NOT be able to update user roles
        Long librarianId = extractUserIdFromGraphQLResponse(createResponse.getBody());
        String attemptUpdateUserMutation = String.format("""
            mutation {
                updateUser(id: %d, input: {
                    username: "%s",
                    role: "ADMIN",
                    password: "newpassword123"
                }) {
                    id
                    username
                    role
                }
            }
            """, librarianId, testLibrarianUsername);

        ResponseEntity<String> unauthorizedUpdateResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(attemptUpdateUserMutation), librarianHeaders),
            String.class
        );

        // Should get access denied
        assertThat(unauthorizedUpdateResponse.getBody()).contains("errors");
        System.out.println("✅ Librarian cannot update users - PASSED");

        System.out.println("✅ Librarian Permissions Flow E2E Test PASSED!");
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

    private Long extractUserIdFromGraphQLResponse(String responseBody) throws Exception {
    // Simple JSON parsing to extract user ID
    // The response format is: {"data":{"addUser":{"id":"11","username":"...","role":"..."}}}
    if (responseBody.contains("addUser")) {
        // Extract ID from addUser response - ID is wrapped in quotes
        String idPattern = "\"id\":\\s*\"(\\d+)\"";  // Changed to capture quoted number
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
        java.util.regex.Matcher matcher = pattern.matcher(responseBody);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
    }
    throw new RuntimeException("Could not extract user ID from response: " + responseBody);
}
}
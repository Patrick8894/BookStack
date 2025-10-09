package com.bookstack.bookstack.e2e;

import com.bookstack.bookstack.auth.dto.LoginRequest;
import com.bookstack.bookstack.auth.dto.LoginResponse;
import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.repository.BookRepository;
import com.bookstack.bookstack.borrow.dto.BorrowRequest;
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
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RoleBasedAccessControlE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private User member;
    private Book testBook;
    
    // Use unique usernames to avoid conflicts
    private String adminUsername;
    private String librarianUsername;
    private String memberUsername;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        
        // Generate unique usernames for each test run
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        adminUsername = "admin_" + uniqueId;
        librarianUsername = "librarian_" + uniqueId;
        memberUsername = "member_" + uniqueId;
        
        // Clean up any existing data (optional, but good practice)
        userRepository.deleteAll();
        bookRepository.deleteAll();
        
        // Create test users with unique usernames
        createUser(adminUsername, "password123", "ADMIN");
        createUser(librarianUsername, "password123", "LIBRARIAN");
        member = createUser(memberUsername, "password123", "MEMBER");
        
        // Create test book with unique ISBN
        testBook = createBook("Access Control Test Book", "Test Author", "978-1-111-" + uniqueId, 3, 3);
    }

    @Test
    void memberAccessControl_ShouldHaveLimitedAccess() throws Exception {
        String memberToken = loginUser(memberUsername, "password123");
        HttpHeaders memberHeaders = createAuthHeaders(memberToken);

        // ✅ Members CAN: View books via GraphQL
        ResponseEntity<String> booksResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery("{ allBooks { id title availableCopies } }"), memberHeaders),
            String.class
        );
        assertThat(booksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isGraphQLForbidden(booksResponse.getBody())).isFalse();
        assertThat(booksResponse.getBody()).contains("Access Control Test Book");
        System.out.println("✅ Member can view books via GraphQL");

        // ✅ Members CAN: View their own borrows 
        ResponseEntity<String> userBorrowsResponse = restTemplate.exchange(
            baseUrl + "/borrows/user/" + member.getId() + "/active",
            HttpMethod.GET,
            new HttpEntity<>(memberHeaders),
            String.class
        );
        assertThat(userBorrowsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Member can view their own borrows");

        // ❌ Members CANNOT: Create borrow records (only librarians/admins can)
        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setUserId(member.getId());
        borrowRequest.setBookId(testBook.getId());
        borrowRequest.setNotes("Unauthorized attempt");

        ResponseEntity<String> createBorrowResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.POST,
            new HttpEntity<>(borrowRequest, memberHeaders),
            String.class
        );
        assertThat(createBorrowResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        System.out.println("✅ Member correctly denied access to create borrows");

        // ❌ Members CANNOT: Access all borrows (admin/librarian only)
        ResponseEntity<String> allBorrowsResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.GET,
            new HttpEntity<>(memberHeaders),
            String.class
        );
        assertThat(allBorrowsResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        System.out.println("✅ Member correctly denied access to all borrows");

        // ❌ Members CANNOT: Access user management via GraphQL
        ResponseEntity<String> allUsersResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery("{ allUsers { id username role } }"), memberHeaders),
            String.class
        );
        assertThat(allUsersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isGraphQLForbidden(allUsersResponse.getBody())).isTrue();
        System.out.println("✅ Member correctly denied access to user management");
    }

    @Test
    void librarianAccessControl_ShouldHaveBookAndBorrowAccess() throws Exception {
        String librarianToken = loginUser(librarianUsername, "password123");
        HttpHeaders librarianHeaders = createAuthHeaders(librarianToken);

        // ✅ Librarians CAN: View books
        ResponseEntity<String> booksResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery("{ allBooks { id title availableCopies } }"), librarianHeaders),
            String.class
        );
        assertThat(booksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isGraphQLForbidden(booksResponse.getBody())).isFalse();
        System.out.println("✅ Librarian can view books");

        // ✅ Librarians CAN: Create borrow records
        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setUserId(member.getId());
        borrowRequest.setBookId(testBook.getId());
        borrowRequest.setNotes("Librarian-created borrow");

        ResponseEntity<String> createBorrowResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.POST,
            new HttpEntity<>(borrowRequest, librarianHeaders),
            String.class
        );
        assertThat(createBorrowResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        System.out.println("✅ Librarian can create borrows");

        // ✅ Librarians CAN: View all borrows
        ResponseEntity<String> allBorrowsResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.GET,
            new HttpEntity<>(librarianHeaders),
            String.class
        );
        assertThat(allBorrowsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Librarian can view all borrows");

        // ✅ Librarians CAN: Access user management (READ-ONLY)
        ResponseEntity<String> allUsersResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery("{ allUsers { id username role } }"), librarianHeaders),
            String.class
        );
        assertThat(allUsersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isGraphQLForbidden(allUsersResponse.getBody())).isFalse(); // Should NOT be forbidden
        assertThat(allUsersResponse.getBody()).contains(adminUsername);
        assertThat(allUsersResponse.getBody()).contains(librarianUsername);
        assertThat(allUsersResponse.getBody()).contains(memberUsername);
        System.out.println("✅ Librarian can read user data (view users)");

        // ❌ Librarians CANNOT: Create users (WRITE operation)
        String createUserMutation = String.format("""
            mutation {
                addUser(input: {
                    username: "test_librarian_created_user_%s",
                    password: "password123",
                    role: "MEMBER"
                }) {
                    id
                    username
                    role
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8));

        ResponseEntity<String> createUserResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(createUserMutation), librarianHeaders),
            String.class
        );
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isGraphQLForbidden(createUserResponse.getBody())).isTrue(); // Should be forbidden
        System.out.println("✅ Librarian correctly denied user creation (write operation)");

        // ❌ Librarians CANNOT: Update users (WRITE operation)
        String updateUserMutation = String.format("""
            mutation {
                updateUser(id: %d, input: {
                    username: "%s",
                    password: "password123",
                    role: "ADMIN"
                }) {
                    id
                    username
                    role
                }
            }
            """, member.getId(), memberUsername);

        ResponseEntity<String> updateUserResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(updateUserMutation), librarianHeaders),
            String.class
        );
        assertThat(updateUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("GraphQL Response Body: " + updateUserResponse.getBody());
        assertThat(isGraphQLForbidden(updateUserResponse.getBody())).isTrue(); // Should be forbidden
        System.out.println("✅ Librarian correctly denied user updates (write operation)");

        // ❌ Librarians CANNOT: Delete users (WRITE operation)
        String deleteUserMutation = String.format("""
            mutation {
                deleteUser(id: %d)
            }
            """, member.getId());

        ResponseEntity<String> deleteUserResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery(deleteUserMutation), librarianHeaders),
            String.class
        );
        assertThat(deleteUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isGraphQLForbidden(deleteUserResponse.getBody())).isTrue(); // Should be forbidden
        System.out.println("✅ Librarian correctly denied user deletion (write operation)");
    }

    @Test
    void adminAccessControl_ShouldHaveFullAccess() throws Exception {
        String adminToken = loginUser(adminUsername, "password123");
        HttpHeaders adminHeaders = createAuthHeaders(adminToken);

        // ✅ Admins CAN: View books
        ResponseEntity<String> booksResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery("{ allBooks { id title availableCopies } }"), adminHeaders),
            String.class
        );
        assertThat(booksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isGraphQLForbidden(booksResponse.getBody())).isFalse();
        System.out.println("✅ Admin can view books");

        // ✅ Admins CAN: Create borrow records
        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setUserId(member.getId());
        borrowRequest.setBookId(testBook.getId());
        borrowRequest.setNotes("Admin-created borrow");

        ResponseEntity<String> createBorrowResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.POST,
            new HttpEntity<>(borrowRequest, adminHeaders),
            String.class
        );
        assertThat(createBorrowResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        System.out.println("✅ Admin can create borrows");

        // ✅ Admins CAN: View all borrows
        ResponseEntity<String> allBorrowsResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.GET,
            new HttpEntity<>(adminHeaders),
            String.class
        );
        assertThat(allBorrowsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("✅ Admin can view all borrows");

        // ✅ Admins CAN: Access user management
        ResponseEntity<String> allUsersResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery("{ allUsers { id username role } }"), adminHeaders),
            String.class
        );
        assertThat(allUsersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isGraphQLForbidden(allUsersResponse.getBody())).isFalse();
        assertThat(allUsersResponse.getBody()).contains(adminUsername);
        assertThat(allUsersResponse.getBody()).contains(librarianUsername);
        assertThat(allUsersResponse.getBody()).contains(memberUsername);
        System.out.println("✅ Admin can access user management");
    }

    @Test
    void unauthorizedAccess_ShouldBeDenied() throws Exception {
        // ❌ No token - should be unauthorized
        ResponseEntity<String> noTokenResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.GET,
            new HttpEntity<>(createHeaders()),
            String.class
        );
        assertThat(noTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        System.out.println("✅ Request without token correctly denied");

        // ❌ Invalid token - should be unauthorized
        HttpHeaders invalidHeaders = createHeaders();
        invalidHeaders.setBearerAuth("invalid.jwt.token");

        ResponseEntity<String> invalidTokenResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.GET,
            new HttpEntity<>(invalidHeaders),
            String.class
        );
        assertThat(invalidTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        System.out.println("✅ Request with invalid token correctly denied");
    }

    // Helper methods
    private String loginUser(String username, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            baseUrl + "/auth/login",
            loginRequest,
            LoginResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse loginResponse = Objects.requireNonNull(response.getBody(), "Login response body is null");
        return loginResponse.getToken();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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

    private Book createBook(String title, String author, String isbn, int totalCopies, int availableCopies) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setCategory("Test Category");
        book.setLanguage("English");
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(availableCopies);
        return bookRepository.save(book);
    }

    @SuppressWarnings("unchecked")
    private boolean isGraphQLForbidden(String body) throws Exception {
        if (body == null) return false;
        Map<String, Object> map = objectMapper.readValue(body, Map.class);
        Object errors = map.get("errors");
        if (errors instanceof Iterable<?> errs) {
            for (Object err : errs) {
                Map<String, Object> e = (Map<String, Object>) err;
                String message = (String) e.get("message");
                Map<String, Object> extensions = (Map<String, Object>) e.get("extensions");
                if ((message != null && message.contains("Insufficient permissions")) ||
                    (extensions != null && "FORBIDDEN".equals(extensions.get("classification")))) {
                    return true;
                }
            }
        }
        return false;
    }
}
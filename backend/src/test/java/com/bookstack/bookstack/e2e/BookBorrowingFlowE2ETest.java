package com.bookstack.bookstack.e2e;

import com.bookstack.bookstack.auth.dto.LoginRequest;
import com.bookstack.bookstack.auth.dto.LoginResponse;
import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.repository.BookRepository;
import com.bookstack.bookstack.borrow.dto.BorrowRequest;
import com.bookstack.bookstack.borrow.dto.BorrowResponse;
import com.bookstack.bookstack.borrow.model.BorrowStatus;
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

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookBorrowingFlowE2ETest {

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
    private Book availableBook;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        
        // Create test users
        createUser("librarian", "password123", "LIBRARIAN"); // Create but don't store reference
        member = createUser("member", "password123", "MEMBER");
        
        // Create test book
        availableBook = createBook("Test Book", "Test Author", "978-1-234-56789-0", 5, 5);
    }

    @Test
    void completeBookBorrowingFlow_ShouldWork() throws Exception {
        // Step 1: Librarian logs in
        String librarianToken = loginUser("librarian", "password123");
        assertThat(librarianToken).isNotNull();

        // Step 2: Member logs in  
        String memberToken = loginUser("member", "password123");
        assertThat(memberToken).isNotNull();

        // Step 3: Member checks available books (via GraphQL - we'll use REST for now)
        HttpHeaders memberHeaders = createAuthHeaders(memberToken);
        ResponseEntity<String> booksResponse = restTemplate.exchange(
            baseUrl + "/graphql",
            HttpMethod.POST,
            new HttpEntity<>(createGraphQLQuery("{ allBooks { id title availableCopies } }"), memberHeaders),
            String.class
        );
        assertThat(booksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(booksResponse.getBody()).contains("Test Book");

        // Step 4: Librarian creates a borrow record for the member
        BorrowRequest borrowRequest = new BorrowRequest();
        borrowRequest.setUserId(member.getId());
        borrowRequest.setBookId(availableBook.getId());
        borrowRequest.setNotes("E2E test borrow");

        HttpHeaders librarianHeaders = createAuthHeaders(librarianToken);
        ResponseEntity<BorrowResponse> borrowResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.POST,
            new HttpEntity<>(borrowRequest, librarianHeaders),
            BorrowResponse.class
        );

        assertThat(borrowResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BorrowResponse borrow = Objects.requireNonNull(borrowResponse.getBody(), "Borrow response body is null");
        assertThat(borrow.getUserId()).isEqualTo(member.getId());
        assertThat(borrow.getBookId()).isEqualTo(availableBook.getId());
        assertThat(borrow.getStatus()).isEqualTo(BorrowStatus.ACTIVE);

        // Step 5: Verify book availability decreased
        Book updatedBook = bookRepository.findById(availableBook.getId()).orElseThrow();
        assertThat(updatedBook.getAvailableCopies()).isEqualTo(4); // Was 5, now 4

        // Step 6: Member checks their active borrows
        ResponseEntity<BorrowResponse[]> memberBorrowsResponse = restTemplate.exchange(
            baseUrl + "/borrows/user/" + member.getId() + "/active",
            HttpMethod.GET,
            new HttpEntity<>(memberHeaders),
            BorrowResponse[].class
        );

        assertThat(memberBorrowsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BorrowResponse[] memberBorrows = Objects.requireNonNull(memberBorrowsResponse.getBody(), "Member borrows response body is null");
        assertThat(memberBorrows).isNotNull(); // Add null check first
        assertThat(memberBorrows).hasSize(1);
        assertThat(memberBorrows[0].getBookTitle()).isEqualTo("Test Book");
        assertThat(memberBorrows[0].getStatus()).isEqualTo(BorrowStatus.ACTIVE);

        System.out.println("✅ Complete Book Borrowing Flow E2E Test PASSED!");
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse loginResponse = Objects.requireNonNull(response.getBody(), "Login response body is null");
        assertThat(loginResponse).isNotNull(); // Add null check
        return loginResponse.getToken();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private String createGraphQLQuery(String query) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("query", query));
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
}
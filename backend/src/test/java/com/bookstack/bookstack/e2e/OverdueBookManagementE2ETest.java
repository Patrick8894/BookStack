package com.bookstack.bookstack.e2e;

import com.bookstack.bookstack.auth.dto.LoginRequest;
import com.bookstack.bookstack.auth.dto.LoginResponse;
import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.repository.BookRepository;
import com.bookstack.bookstack.borrow.dto.BorrowResponse;
import com.bookstack.bookstack.borrow.model.Borrow;
import com.bookstack.bookstack.borrow.model.BorrowStatus;
import com.bookstack.bookstack.borrow.repository.BorrowRepository;
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

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OverdueBookManagementE2ETest {

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
    private BorrowRepository borrowRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private User member;
    private Book testBook;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        
        // Clean up existing data to avoid constraint violations
        borrowRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test users with unique names
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        createUser("librarian_" + uniqueId, "password123", "LIBRARIAN");
        member = createUser("member_" + uniqueId, "password123", "MEMBER");
        
        // Create test book
        testBook = createBook("Overdue Test Book " + uniqueId, "Test Author", "978-234-589-" + uniqueId, 3, 3);
    }

    @Test
    void overdueBookManagementFlow_ShouldWork() throws Exception {
        // Step 1: Librarian logs in
        String librarianUsername = "librarian_" + UUID.randomUUID().toString().substring(0, 8);
        createUser(librarianUsername, "password123", "LIBRARIAN");
        String librarianToken = loginUser(librarianUsername, "password123");
        assertThat(librarianToken).isNotNull();

        // Step 2: Create a borrow record that's already overdue (simulate past borrow)
        Borrow overdueBorrow = createOverdueBorrow();
        
        // Step 3: Member logs in
        String memberToken = loginUser(member.getUsername(), "password123");
        assertThat(memberToken).isNotNull();

        // Step 4: Librarian checks overdue books (this should trigger status update)
        HttpHeaders librarianHeaders = createAuthHeaders(librarianToken);
        ResponseEntity<BorrowResponse[]> overdueResponse = restTemplate.exchange(
            baseUrl + "/borrows/overdue",
            HttpMethod.GET,
            new HttpEntity<>(librarianHeaders),
            BorrowResponse[].class
        );

        assertThat(overdueResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BorrowResponse[] overdueBooks = Objects.requireNonNull(overdueResponse.getBody(), "Overdue response body is null");
        assertThat(overdueBooks).hasSizeGreaterThanOrEqualTo(1);
        
        // Find our test overdue book
        BorrowResponse overdueBook = null;
        for (BorrowResponse borrow : overdueBooks) {
            if (borrow.getId().equals(overdueBorrow.getId())) {
                overdueBook = borrow;
                break;
            }
        }
        
        Objects.requireNonNull(overdueBook, "Overdue book not found in response");
        assertThat(overdueBook.getStatus()).isEqualTo(BorrowStatus.OVERDUE);
        assertThat(overdueBook.getUserId()).isEqualTo(member.getId());
        assertThat(overdueBook.getBookId()).isEqualTo(testBook.getId());

        // Step 5: Verify the borrow status was automatically updated in database
        Borrow updatedBorrow = borrowRepository.findById(overdueBorrow.getId()).orElseThrow();
        assertThat(updatedBorrow.getStatus()).isEqualTo(BorrowStatus.OVERDUE);

        // Step 6: Member checks their borrows (should see overdue status)
        HttpHeaders memberHeaders = createAuthHeaders(memberToken);
        ResponseEntity<BorrowResponse[]> memberBorrowsResponse = restTemplate.exchange(
            baseUrl + "/borrows/user/" + member.getId(),
            HttpMethod.GET,
            new HttpEntity<>(memberHeaders),
            BorrowResponse[].class
        );

        assertThat(memberBorrowsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BorrowResponse[] memberBorrows = Objects.requireNonNull(memberBorrowsResponse.getBody(), "Member borrows response body is null");
        
        // Find the overdue borrow in member's list
        BorrowResponse memberOverdueBorrow = null;
        for (BorrowResponse borrow : memberBorrows) {
            if (borrow.getId().equals(overdueBorrow.getId())) {
                memberOverdueBorrow = borrow;
                break;
            }
        }
        
        Objects.requireNonNull(memberOverdueBorrow, "Member's overdue borrow not found");
        assertThat(memberOverdueBorrow.getStatus()).isEqualTo(BorrowStatus.OVERDUE);

        // Step 7: Member/Librarian returns the overdue book (late return)
        String returnNotes = "Returned late - apologies for the delay";
        ResponseEntity<BorrowResponse> returnResponse = restTemplate.exchange(
            baseUrl + "/borrows/" + overdueBorrow.getId() + "/return",
            HttpMethod.PUT,
            new HttpEntity<>(createReturnRequest(returnNotes), librarianHeaders),
            BorrowResponse.class
        );

        assertThat(returnResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BorrowResponse returnedBorrow = Objects.requireNonNull(returnResponse.getBody(), "Return response body is null");
        assertThat(returnedBorrow.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(returnedBorrow.getReturnDate()).isNotNull();
        assertThat(returnedBorrow.getNotes()).contains(returnNotes);

        // Step 8: Verify book availability increased after return
        Book updatedBook = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(updatedBook.getAvailableCopies()).isEqualTo(3); // Back to original

        // Step 9: Verify overdue book no longer appears in overdue list
        ResponseEntity<BorrowResponse[]> finalOverdueResponse = restTemplate.exchange(
            baseUrl + "/borrows/overdue",
            HttpMethod.GET,
            new HttpEntity<>(librarianHeaders),
            BorrowResponse[].class
        );

        assertThat(finalOverdueResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BorrowResponse[] finalOverdueBooks = Objects.requireNonNull(finalOverdueResponse.getBody(), "Final overdue response body is null");
        
        // Our returned book should not be in overdue list anymore
        boolean foundReturnedBookInOverdue = false;
        for (BorrowResponse borrow : finalOverdueBooks) {
            if (borrow.getId().equals(overdueBorrow.getId())) {
                foundReturnedBookInOverdue = true;
                break;
            }
        }
        assertThat(foundReturnedBookInOverdue).isFalse();

        System.out.println("✅ Overdue Book Management Flow E2E Test PASSED!");
    }

    @Test
    void multipleConcurrentOverdueBooks_ShouldAllBeUpdated() throws Exception {
        // Create unique librarian for this test
        String librarianUsername = "librarian_" + UUID.randomUUID().toString().substring(0, 8);
        createUser(librarianUsername, "password123", "LIBRARIAN");
        
        // Create multiple overdue borrows
        createOverdueBorrow();
        createOverdueBorrow();

        String librarianToken = loginUser(librarianUsername, "password123");
        HttpHeaders librarianHeaders = createAuthHeaders(librarianToken);

        // Trigger overdue check
        ResponseEntity<BorrowResponse[]> overdueResponse = restTemplate.exchange(
            baseUrl + "/borrows/overdue",
            HttpMethod.GET,
            new HttpEntity<>(librarianHeaders),
            BorrowResponse[].class
        );

        assertThat(overdueResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BorrowResponse[] overdueBooks = Objects.requireNonNull(overdueResponse.getBody(), "Overdue response body is null");
        
        // Should have at least our 2 overdue books
        assertThat(overdueBooks.length).isGreaterThanOrEqualTo(2);
        
        // All should have OVERDUE status
        for (BorrowResponse borrow : overdueBooks) {
            assertThat(borrow.getStatus()).isEqualTo(BorrowStatus.OVERDUE);
        }

        System.out.println("✅ Multiple Concurrent Overdue Books Test PASSED!");
    }

    // Helper methods
    private Borrow createOverdueBorrow() {
        // Create a borrow that's already overdue (due date in the past)
        LocalDateTime borrowDate = LocalDateTime.now().minusDays(20);
        LocalDateTime dueDate = LocalDateTime.now().minusDays(6); // 6 days overdue
        
        Borrow borrow = new Borrow(member, testBook, borrowDate, dueDate);
        borrow.setStatus(BorrowStatus.ACTIVE); // Still active but overdue
        borrow.setNotes("Test overdue borrow");
        
        // Decrease book availability
        testBook.setAvailableCopies(testBook.getAvailableCopies() - 1);
        bookRepository.save(testBook);
        
        return borrowRepository.save(borrow);
    }

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
        return loginResponse.getToken();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private String createReturnRequest(String notes) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("notes", notes));
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
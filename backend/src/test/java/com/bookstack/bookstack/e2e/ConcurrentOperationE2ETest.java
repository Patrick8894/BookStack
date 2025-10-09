package com.bookstack.bookstack.e2e;

import com.bookstack.bookstack.auth.dto.LoginRequest;
import com.bookstack.bookstack.auth.dto.LoginResponse;
import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.repository.BookRepository;
import com.bookstack.bookstack.borrow.dto.BorrowRequest;
import com.bookstack.bookstack.borrow.dto.BorrowResponse;
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

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConcurrentOperationsE2ETest {

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
    private User librarian;
    private Book limitedBook; // Book with only 1 copy

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        
        // Clean up existing data
        borrowRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test users with unique names
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        librarian = createUser("librarian_" + uniqueId, "password123", "LIBRARIAN");
        
        // Create test books
        limitedBook = createBook("Limited Edition Book " + uniqueId, "Rare Author", "978-111-" + uniqueId.substring(0, 6), 1, 1); // Only 1 copy
        createBook("Popular Book " + uniqueId, "Famous Author", "978-222-" + uniqueId.substring(0, 6), 5, 5); // 5 copies
    }

    @Test
    void concurrentBorrowingLastCopy_OnlyOneUserShouldSucceed() throws Exception {
        // Create multiple users who will try to borrow simultaneously
        User member1 = createUser("member1_" + UUID.randomUUID().toString().substring(0, 8), "password123", "MEMBER");
        User member2 = createUser("member2_" + UUID.randomUUID().toString().substring(0, 8), "password123", "MEMBER");
        User member3 = createUser("member3_" + UUID.randomUUID().toString().substring(0, 8), "password123", "MEMBER");

        // All users log in
        String librarianToken = loginUser(librarian.getUsername(), "password123");
        loginUser(member1.getUsername(), "password123");
        loginUser(member2.getUsername(), "password123");
        loginUser(member3.getUsername(), "password123");

        HttpHeaders librarianHeaders = createAuthHeaders(librarianToken);

        // Verify book has only 1 available copy
        Book bookBefore = bookRepository.findById(limitedBook.getId()).orElseThrow();
        assertThat(bookBefore.getAvailableCopies()).isEqualTo(1);

        // Create concurrent borrow requests
        BorrowRequest request1 = createBorrowRequest(member1.getId(), limitedBook.getId(), "Member 1 wants this book");
        BorrowRequest request2 = createBorrowRequest(member2.getId(), limitedBook.getId(), "Member 2 wants this book");
        BorrowRequest request3 = createBorrowRequest(member3.getId(), limitedBook.getId(), "Member 3 wants this book");

        // Execute concurrent borrow attempts
        ExecutorService executor = Executors.newFixedThreadPool(3);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                ResponseEntity<BorrowResponse> response = restTemplate.exchange(
                    baseUrl + "/borrows",
                    HttpMethod.POST,
                    new HttpEntity<>(request1, librarianHeaders),
                    BorrowResponse.class
                );
                if (response.getStatusCode() == HttpStatus.CREATED) {
                    successCount.incrementAndGet();
                    System.out.println("Member 1 borrow: SUCCESS");
                } else {
                    failureCount.incrementAndGet();
                    System.out.println("Member 1 borrow: FAILED - " + response.getStatusCode());
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
                System.out.println("Member 1 borrow: EXCEPTION - " + e.getMessage());
            }
        }, executor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                ResponseEntity<BorrowResponse> response = restTemplate.exchange(
                    baseUrl + "/borrows",
                    HttpMethod.POST,
                    new HttpEntity<>(request2, librarianHeaders),
                    BorrowResponse.class
                );
                if (response.getStatusCode() == HttpStatus.CREATED) {
                    successCount.incrementAndGet();
                    System.out.println("Member 2 borrow: SUCCESS");
                } else {
                    failureCount.incrementAndGet();
                    System.out.println("Member 2 borrow: FAILED - " + response.getStatusCode());
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
                System.out.println("Member 2 borrow: EXCEPTION - " + e.getMessage());
            }
        }, executor);

        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> {
            try {
                ResponseEntity<BorrowResponse> response = restTemplate.exchange(
                    baseUrl + "/borrows",
                    HttpMethod.POST,
                    new HttpEntity<>(request3, librarianHeaders),
                    BorrowResponse.class
                );
                if (response.getStatusCode() == HttpStatus.CREATED) {
                    successCount.incrementAndGet();
                    System.out.println("Member 3 borrow: SUCCESS");
                } else {
                    failureCount.incrementAndGet();
                    System.out.println("Member 3 borrow: FAILED - " + response.getStatusCode());
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
                System.out.println("Member 3 borrow: EXCEPTION - " + e.getMessage());
            }
        }, executor);

        // Wait for all requests to complete
        CompletableFuture.allOf(future1, future2, future3).join();
        executor.shutdown();

        // Verify results: Only 1 should succeed, 2 should fail
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(2);

        // Verify book availability is now 0
        Book bookAfter = bookRepository.findById(limitedBook.getId()).orElseThrow();
        assertThat(bookAfter.getAvailableCopies()).isEqualTo(0);

        // Verify only 1 active borrow exists for this book
        long activeBorrowCount = borrowRepository.findAll().stream()
            .filter(borrow -> borrow.getBook().getId().equals(limitedBook.getId()))
            .filter(borrow -> borrow.getStatus() == BorrowStatus.ACTIVE)
            .count();
        assertThat(activeBorrowCount).isEqualTo(1);

        System.out.println("✅ Concurrent Borrowing Last Copy Test PASSED!");
    }

    @Test
    void concurrentReturnAndBorrow_ShouldHandleRaceCondition() throws Exception {
        // Setup: Create a borrow that will be returned
        User member1 = createUser("borrower_" + UUID.randomUUID().toString().substring(0, 8), "password123", "MEMBER");
        User member2 = createUser("waiter_" + UUID.randomUUID().toString().substring(0, 8), "password123", "MEMBER");

        String librarianToken = loginUser(librarian.getUsername(), "password123");
        HttpHeaders librarianHeaders = createAuthHeaders(librarianToken);

        // Member1 borrows the only copy
        BorrowRequest initialBorrow = createBorrowRequest(member1.getId(), limitedBook.getId(), "Initial borrow");
        ResponseEntity<BorrowResponse> borrowResponse = restTemplate.exchange(
            baseUrl + "/borrows",
            HttpMethod.POST,
            new HttpEntity<>(initialBorrow, librarianHeaders),
            BorrowResponse.class
        );

        assertThat(borrowResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BorrowResponse activeBorrow = Objects.requireNonNull(borrowResponse.getBody());
        
        // Verify no copies available
        Book bookWithNoCopies = bookRepository.findById(limitedBook.getId()).orElseThrow();
        assertThat(bookWithNoCopies.getAvailableCopies()).isEqualTo(0);

        // Concurrent operations: Return book AND try to borrow it
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger returnSuccess = new AtomicInteger(0);
        AtomicInteger borrowSuccess = new AtomicInteger(0);

        // Return operation
        CompletableFuture<Void> returnFuture = CompletableFuture.runAsync(() -> {
            try {
                String returnRequest = objectMapper.writeValueAsString(java.util.Map.of("notes", "Returning book"));
                ResponseEntity<BorrowResponse> returnResponse = restTemplate.exchange(
                    baseUrl + "/borrows/" + activeBorrow.getId() + "/return",
                    HttpMethod.PUT,
                    new HttpEntity<>(returnRequest, librarianHeaders),
                    BorrowResponse.class
                );
                if (returnResponse.getStatusCode() == HttpStatus.OK) {
                    returnSuccess.incrementAndGet();
                    System.out.println("Return operation: SUCCESS");
                }
            } catch (Exception e) {
                System.out.println("Return operation: FAILED - " + e.getMessage());
            }
        }, executor);

        // Borrow operation (member2 trying to borrow)
        CompletableFuture<Void> borrowFuture = CompletableFuture.runAsync(() -> {
            try {
                BorrowRequest newBorrow = createBorrowRequest(member2.getId(), limitedBook.getId(), "Trying to borrow returned book");
                ResponseEntity<BorrowResponse> newBorrowResponse = restTemplate.exchange(
                    baseUrl + "/borrows",
                    HttpMethod.POST,
                    new HttpEntity<>(newBorrow, librarianHeaders),
                    BorrowResponse.class
                );
                if (newBorrowResponse.getStatusCode() == HttpStatus.CREATED) {
                    borrowSuccess.incrementAndGet();
                    System.out.println("New borrow operation: SUCCESS");
                }
            } catch (Exception e) {
                System.out.println("New borrow operation: FAILED - " + e.getMessage());
            }
        }, executor);

        // Wait for both operations
        CompletableFuture.allOf(returnFuture, borrowFuture).join();
        executor.shutdown();

        // Verify final state
        Book finalBook = bookRepository.findById(limitedBook.getId()).orElseThrow();
        
        if (borrowSuccess.get() == 1) {
            // If new borrow succeeded, book should have 0 available copies
            assertThat(finalBook.getAvailableCopies()).isEqualTo(0);
            System.out.println("Race condition: New borrow succeeded immediately after return");
        } else {
            // If new borrow failed, book should have 1 available copy
            assertThat(finalBook.getAvailableCopies()).isEqualTo(1);
            System.out.println("Race condition: Return completed but new borrow failed/didn't execute in time");
        }

        // Return should always succeed
        assertThat(returnSuccess.get()).isEqualTo(1);

        System.out.println("✅ Concurrent Return and Borrow Test PASSED!");
    }

    // Helper methods
    private BorrowRequest createBorrowRequest(Long userId, Long bookId, String notes) {
        BorrowRequest request = new BorrowRequest();
        request.setUserId(userId);
        request.setBookId(bookId);
        request.setNotes(notes);
        return request;
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
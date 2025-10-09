package com.bookstack.bookstack.borrow.service;

import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.service.BookService;
import com.bookstack.bookstack.borrow.dto.BorrowResponse;
import com.bookstack.bookstack.borrow.mapper.BorrowMapper;
import com.bookstack.bookstack.borrow.model.Borrow;
import com.bookstack.bookstack.borrow.model.BorrowStatus;
import com.bookstack.bookstack.borrow.repository.BorrowRepository;
import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.NotFoundException;
import com.bookstack.bookstack.common.lock.GlobalBorrowLock;
import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.quality.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
class BorrowServiceTest {

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private UserService userService;

    @Mock
    private BookService bookService;

    @Mock
    private BorrowMapper borrowMapper;

    @Mock
    private GlobalBorrowLock globalLock; 

    @InjectMocks
    private BorrowService borrowService;

    private User validUser;
    private Book validBook;
    private Borrow activeBorrow;
    private Borrow returnedBorrow;
    private BorrowResponse borrowResponse;
    private List<Borrow> borrowList;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Configure GlobalBorrowLock mock to always succeed
        when(globalLock.tryLock(5000L)).thenReturn(true); // Match the exact value from service
        doNothing().when(globalLock).unlock();

        validUser = new User();
        validUser.setId(1L);
        validUser.setUsername("john_doe");
        validUser.setRole("MEMBER");

        validBook = new Book();
        validBook.setId(1L);
        validBook.setTitle("The Great Gatsby");
        validBook.setAuthor("F. Scott Fitzgerald");
        validBook.setTotalCopies(5);
        validBook.setAvailableCopies(3);

        activeBorrow = new Borrow(validUser, validBook, LocalDateTime.now(), LocalDateTime.now().plusDays(14));
        activeBorrow.setId(1L);
        activeBorrow.setStatus(BorrowStatus.ACTIVE);
        activeBorrow.setNotes("Test borrow");

        returnedBorrow = new Borrow(validUser, validBook, LocalDateTime.now().minusDays(7), LocalDateTime.now().plusDays(7));
        returnedBorrow.setId(2L);
        returnedBorrow.setStatus(BorrowStatus.RETURNED);
        returnedBorrow.setReturnDate(LocalDateTime.now());

        borrowResponse = new BorrowResponse();
        borrowResponse.setId(1L);
        borrowResponse.setUserId(1L);
        borrowResponse.setBookId(1L);
        borrowResponse.setStatus(BorrowStatus.ACTIVE);
        borrowResponse.setNotes("Test borrow");

        borrowList = Arrays.asList(activeBorrow, returnedBorrow);
    }

    // ===== BORROW BOOK TESTS =====

    @Test
    void borrowBook_WithValidData_ShouldCreateBorrow() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        String notes = "Test borrow";

        when(userService.getUserById(userId)).thenReturn(validUser);
        when(bookService.getBookById(bookId)).thenReturn(validBook);
        when(borrowRepository.findActiveBorrowByUserAndBook(userId, bookId)).thenReturn(Collections.emptyList());
        when(borrowRepository.save(any(Borrow.class))).thenReturn(activeBorrow);
        when(borrowMapper.toResponse(activeBorrow)).thenReturn(borrowResponse);
        when(bookService.updateBook(anyLong(), any(Book.class))).thenReturn(validBook);

        // When
        BorrowResponse result = borrowService.borrowBook(userId, bookId, notes);

        // Then
        assertEquals(borrowResponse, result);
        verify(userService).getUserById(userId);
        verify(bookService).getBookById(bookId);
        verify(borrowRepository).findActiveBorrowByUserAndBook(userId, bookId);
        verify(borrowRepository).save(any(Borrow.class));
        verify(bookService).updateBook(bookId, validBook);
        verify(borrowMapper).toResponse(activeBorrow);

        // Verify book availability was decreased
        assertEquals(2, validBook.getAvailableCopies());
    }

    @Test
    void borrowBook_WithNoAvailableCopies_ShouldThrowBadRequestException() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        String notes = "Test borrow";

        validBook.setAvailableCopies(0); // No available copies

        when(userService.getUserById(userId)).thenReturn(validUser);
        when(bookService.getBookById(bookId)).thenReturn(validBook);

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> borrowService.borrowBook(userId, bookId, notes));

        assertEquals("Book is not available for borrowing", exception.getMessage());
        verify(userService).getUserById(userId);
        verify(bookService).getBookById(bookId);
        verify(borrowRepository, never()).save(any());
    }

    @Test
    void borrowBook_WithExistingActiveBorrow_ShouldThrowBadRequestException() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        String notes = "Test borrow";

        when(userService.getUserById(userId)).thenReturn(validUser);
        when(bookService.getBookById(bookId)).thenReturn(validBook);
        when(borrowRepository.findActiveBorrowByUserAndBook(userId, bookId))
            .thenReturn(Collections.singletonList(activeBorrow));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> borrowService.borrowBook(userId, bookId, notes));

        assertEquals("User already has this book borrowed", exception.getMessage());
        verify(userService).getUserById(userId);
        verify(bookService).getBookById(bookId);
        verify(borrowRepository).findActiveBorrowByUserAndBook(userId, bookId);
        verify(borrowRepository, never()).save(any());
    }

    @Test
    void borrowBook_WithInvalidUserId_ShouldThrowNotFoundException() {
        // Given
        Long userId = 999L;
        Long bookId = 1L;
        String notes = "Test borrow";

        when(userService.getUserById(userId)).thenThrow(new NotFoundException("User not found"));

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> borrowService.borrowBook(userId, bookId, notes));

        assertEquals("User not found", exception.getMessage());
        verify(userService).getUserById(userId);
        verify(bookService, never()).getBookById(any());
    }

    @Test
    void borrowBook_WithInvalidBookId_ShouldThrowNotFoundException() {
        // Given
        Long userId = 1L;
        Long bookId = 999L;
        String notes = "Test borrow";

        when(userService.getUserById(userId)).thenReturn(validUser);
        when(bookService.getBookById(bookId)).thenThrow(new NotFoundException("Book not found"));

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> borrowService.borrowBook(userId, bookId, notes));

        assertEquals("Book not found", exception.getMessage());
        verify(userService).getUserById(userId);
        verify(bookService).getBookById(bookId);
        verify(borrowRepository, never()).save(any());
    }

    // ===== RETURN BOOK TESTS =====

    @Test
    void returnBook_WithActiveBorrow_ShouldReturnBook() {
        // Given
        Long borrowId = 1L;
        String notes = "Book in good condition";

        when(borrowRepository.findByIdWithUserAndBook(borrowId)).thenReturn(Optional.of(activeBorrow));
        when(borrowRepository.save(activeBorrow)).thenReturn(activeBorrow);
        when(borrowMapper.toResponse(activeBorrow)).thenReturn(borrowResponse);
        when(bookService.updateBook(anyLong(), any(Book.class))).thenReturn(validBook);

        // When
        BorrowResponse result = borrowService.returnBook(borrowId, notes);

        // Then
        assertEquals(borrowResponse, result);
        assertEquals(BorrowStatus.RETURNED, activeBorrow.getStatus());
        assertNotNull(activeBorrow.getReturnDate());
        assertTrue(activeBorrow.getNotes().contains("Return notes: " + notes));

        verify(borrowRepository).findByIdWithUserAndBook(borrowId);
        verify(borrowRepository).save(activeBorrow);
        verify(bookService).updateBook(validBook.getId(), validBook);
        verify(borrowMapper).toResponse(activeBorrow);

        // Verify book availability was increased
        assertEquals(4, validBook.getAvailableCopies());
    }

    @Test
    void returnBook_WithAlreadyReturnedBorrow_ShouldThrowBadRequestException() {
        // Given
        Long borrowId = 2L;
        String notes = "Book in good condition";

        returnedBorrow.setStatus(BorrowStatus.RETURNED);
        when(borrowRepository.findByIdWithUserAndBook(borrowId)).thenReturn(Optional.of(returnedBorrow));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> borrowService.returnBook(borrowId, notes));

        assertEquals("Book is already returned", exception.getMessage());
        verify(borrowRepository).findByIdWithUserAndBook(borrowId);
        verify(borrowRepository, never()).save(any());
    }

    @Test
    void returnBook_WithInvalidBorrowId_ShouldThrowNotFoundException() {
        // Given
        Long borrowId = 999L;
        String notes = "Book in good condition";

        when(borrowRepository.findByIdWithUserAndBook(borrowId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> borrowService.returnBook(borrowId, notes));

        assertEquals("Borrow record not found with id: " + borrowId, exception.getMessage());
        verify(borrowRepository).findByIdWithUserAndBook(borrowId);
        verify(borrowRepository, never()).save(any());
    }

    @Test
    void returnBook_WithNullNotes_ShouldReturnWithoutAddingNotes() {
        // Given
        Long borrowId = 1L;
        String originalNotes = activeBorrow.getNotes();

        when(borrowRepository.findByIdWithUserAndBook(borrowId)).thenReturn(Optional.of(activeBorrow));
        when(borrowRepository.save(activeBorrow)).thenReturn(activeBorrow);
        when(borrowMapper.toResponse(activeBorrow)).thenReturn(borrowResponse);
        when(bookService.updateBook(anyLong(), any(Book.class))).thenReturn(validBook);

        // When
        BorrowResponse result = borrowService.returnBook(borrowId, null);

        // Then
        assertEquals(borrowResponse, result);
        assertEquals(originalNotes, activeBorrow.getNotes()); // Notes unchanged
        verify(borrowRepository).save(activeBorrow);
    }

    // ===== GET BORROW TESTS =====

    @Test
    void getAllBorrows_ShouldReturnAllBorrows() {
        // Given
        when(borrowRepository.findAllWithUserAndBook()).thenReturn(borrowList);
        when(borrowMapper.toResponse(any(Borrow.class))).thenReturn(borrowResponse);

        // When
        List<BorrowResponse> result = borrowService.getAllBorrows();

        // Then
        assertEquals(2, result.size());
        verify(borrowRepository).findAllWithUserAndBook();
        verify(borrowMapper, times(2)).toResponse(any(Borrow.class));
    }

    @Test
    void getBorrowById_WithValidId_ShouldReturnBorrow() {
        // Given
        Long borrowId = 1L;
        when(borrowRepository.findByIdWithUserAndBook(borrowId)).thenReturn(Optional.of(activeBorrow));
        when(borrowMapper.toResponse(activeBorrow)).thenReturn(borrowResponse);

        // When
        BorrowResponse result = borrowService.getBorrowById(borrowId);

        // Then
        assertEquals(borrowResponse, result);
        verify(borrowRepository).findByIdWithUserAndBook(borrowId);
        verify(borrowMapper).toResponse(activeBorrow);
    }

    @Test
    void getBorrowById_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        Long borrowId = 999L;
        when(borrowRepository.findByIdWithUserAndBook(borrowId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> borrowService.getBorrowById(borrowId));

        assertEquals("Borrow record not found with id: " + borrowId, exception.getMessage());
        verify(borrowRepository).findByIdWithUserAndBook(borrowId);
        verify(borrowMapper, never()).toResponse(any());
    }

    @Test
    void getBorrowsByUserId_ShouldReturnUserBorrows() {
        // Given
        Long userId = 1L;
        when(borrowRepository.findByUserIdWithUserAndBook(userId)).thenReturn(borrowList);
        when(borrowMapper.toResponse(any(Borrow.class))).thenReturn(borrowResponse);

        // When
        List<BorrowResponse> result = borrowService.getBorrowsByUserId(userId);

        // Then
        assertEquals(2, result.size());
        verify(borrowRepository).findByUserIdWithUserAndBook(userId);
        verify(borrowMapper, times(2)).toResponse(any(Borrow.class));
    }

    @Test
    void getBorrowsByBookId_ShouldReturnBookBorrows() {
        // Given
        Long bookId = 1L;
        when(borrowRepository.findByBookIdWithUserAndBook(bookId)).thenReturn(borrowList);
        when(borrowMapper.toResponse(any(Borrow.class))).thenReturn(borrowResponse);

        // When
        List<BorrowResponse> result = borrowService.getBorrowsByBookId(bookId);

        // Then
        assertEquals(2, result.size());
        verify(borrowRepository).findByBookIdWithUserAndBook(bookId);
        verify(borrowMapper, times(2)).toResponse(any(Borrow.class));
    }

    @Test
    void getBorrowsByStatus_ShouldReturnBorrowsWithStatus() {
        // Given
        BorrowStatus status = BorrowStatus.ACTIVE;
        when(borrowRepository.findByStatusWithUserAndBook(status)).thenReturn(Collections.singletonList(activeBorrow));
        when(borrowMapper.toResponse(activeBorrow)).thenReturn(borrowResponse);

        // When
        List<BorrowResponse> result = borrowService.getBorrowsByStatus(status);

        // Then
        assertEquals(1, result.size());
        verify(borrowRepository).findByStatusWithUserAndBook(status);
        verify(borrowMapper).toResponse(activeBorrow);
    }

    @Test
    void getActiveBorrowsByUserId_ShouldReturnActiveUserBorrows() {
        // Given
        Long userId = 1L;
        when(borrowRepository.findByUserIdAndStatusWithUserAndBook(userId, BorrowStatus.ACTIVE))
            .thenReturn(Collections.singletonList(activeBorrow));
        when(borrowMapper.toResponse(activeBorrow)).thenReturn(borrowResponse);

        // When
        List<BorrowResponse> result = borrowService.getActiveBorrowsByUserId(userId);

        // Then
        assertEquals(1, result.size());
        verify(borrowRepository).findByUserIdAndStatusWithUserAndBook(userId, BorrowStatus.ACTIVE);
        verify(borrowMapper).toResponse(activeBorrow);
    }

    @Test
    void getOverdueBorrows_ShouldReturnAndUpdateOverdueBorrows() {
        // Given
        Borrow overdueBorrow = new Borrow(validUser, validBook, LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(6));
        overdueBorrow.setId(3L);
        overdueBorrow.setStatus(BorrowStatus.ACTIVE); // Still active but overdue

        when(borrowRepository.findOverdueBorrowsWithUserAndBook(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(overdueBorrow));
        when(borrowRepository.save(overdueBorrow)).thenReturn(overdueBorrow);
        when(borrowMapper.toResponse(overdueBorrow)).thenReturn(borrowResponse);

        // When
        List<BorrowResponse> result = borrowService.getOverdueBorrows();

        // Then
        assertEquals(1, result.size());
        assertEquals(BorrowStatus.OVERDUE, overdueBorrow.getStatus()); // Status should be updated
        verify(borrowRepository).findOverdueBorrowsWithUserAndBook(any(LocalDateTime.class));
        verify(borrowRepository).save(overdueBorrow);
        verify(borrowMapper).toResponse(overdueBorrow);
    }

    @Test
    void getOverdueBorrows_WithAlreadyOverdueBorrows_ShouldNotUpdateStatus() {
        // Given
        Borrow alreadyOverdueBorrow = new Borrow(validUser, validBook, LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(6));
        alreadyOverdueBorrow.setId(3L);
        alreadyOverdueBorrow.setStatus(BorrowStatus.OVERDUE); // Already overdue

        when(borrowRepository.findOverdueBorrowsWithUserAndBook(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(alreadyOverdueBorrow));
        when(borrowMapper.toResponse(alreadyOverdueBorrow)).thenReturn(borrowResponse);

        // When
        List<BorrowResponse> result = borrowService.getOverdueBorrows();

        // Then
        assertEquals(1, result.size());
        verify(borrowRepository).findOverdueBorrowsWithUserAndBook(any(LocalDateTime.class));
        verify(borrowRepository, never()).save(alreadyOverdueBorrow); // Should not save again
        verify(borrowMapper).toResponse(alreadyOverdueBorrow);
    }

    // ===== DELETE BORROW TESTS =====

    @Test
    void deleteBorrow_WithActiveBorrow_ShouldDeleteAndRestoreBookAvailability() {
        // Given
        Long borrowId = 1L;
        when(borrowRepository.findByIdWithUserAndBook(borrowId)).thenReturn(Optional.of(activeBorrow));
        when(bookService.updateBook(anyLong(), any(Book.class))).thenReturn(validBook);
        doNothing().when(borrowRepository).deleteById(borrowId);

        // When
        borrowService.deleteBorrow(borrowId);

        // Then
        verify(borrowRepository).findByIdWithUserAndBook(borrowId);
        verify(bookService).updateBook(validBook.getId(), validBook);
        verify(borrowRepository).deleteById(borrowId);

        // Verify book availability was increased
        assertEquals(4, validBook.getAvailableCopies());
    }

    @Test
    void deleteBorrow_WithReturnedBorrow_ShouldDeleteWithoutRestoringAvailability() {
        // Given
        Long borrowId = 2L;
        int originalAvailableCopies = validBook.getAvailableCopies();

        when(borrowRepository.findByIdWithUserAndBook(borrowId)).thenReturn(Optional.of(returnedBorrow));
        doNothing().when(borrowRepository).deleteById(borrowId);

        // When
        borrowService.deleteBorrow(borrowId);

        // Then
        verify(borrowRepository).findByIdWithUserAndBook(borrowId);
        verify(borrowRepository).deleteById(borrowId);
        verify(bookService, never()).updateBook(any(), any()); // Should not update book availability

        // Verify book availability unchanged
        assertEquals(originalAvailableCopies, validBook.getAvailableCopies());
    }

    @Test
    void deleteBorrow_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        Long borrowId = 999L;
        when(borrowRepository.findByIdWithUserAndBook(borrowId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> borrowService.deleteBorrow(borrowId));

        assertEquals("Borrow record not found with id: " + borrowId, exception.getMessage());
        verify(borrowRepository).findByIdWithUserAndBook(borrowId);
        verify(borrowRepository, never()).deleteById(any());
    }
}
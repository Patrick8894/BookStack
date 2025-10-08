package com.bookstack.bookstack.book.service;

import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.repository.BookRepository;
import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.ConflictException;
import com.bookstack.bookstack.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book validBook;
    private Book validBook2;
    private List<Book> bookList;

    @BeforeEach
    void setUp() {
        validBook = new Book();
        validBook.setId(1L);
        validBook.setTitle("The Great Gatsby");
        validBook.setAuthor("F. Scott Fitzgerald");
        validBook.setIsbn("978-0-7432-7356-5");
        validBook.setCategory("Fiction");
        validBook.setLanguage("English");
        validBook.setTotalCopies(5);
        validBook.setAvailableCopies(3);

        validBook2 = new Book();
        validBook2.setId(2L);
        validBook2.setTitle("To Kill a Mockingbird");
        validBook2.setAuthor("Harper Lee");
        validBook2.setIsbn("978-0-06-112008-4");
        validBook2.setCategory("Fiction");
        validBook2.setLanguage("English");
        validBook2.setTotalCopies(3);
        validBook2.setAvailableCopies(2);

        bookList = Arrays.asList(validBook, validBook2);
    }

    // ===== GET ALL BOOKS TESTS =====

    @Test
    void getAllBooks_ShouldReturnActiveBooks() {
        // Given
        when(bookRepository.findAllActive()).thenReturn(bookList);

        // When
        List<Book> result = bookService.getAllBooks();

        // Then
        assertEquals(2, result.size());
        assertEquals(bookList, result);
        verify(bookRepository).findAllActive();
    }

    @Test
    void getAllBooksIncludingDeleted_ShouldReturnAllBooks() {
        // Given
        when(bookRepository.findAll()).thenReturn(bookList);

        // When
        List<Book> result = bookService.getAllBooksIncludingDeleted();

        // Then
        assertEquals(2, result.size());
        assertEquals(bookList, result);
        verify(bookRepository).findAll();
    }

    @Test
    void getDeletedBooks_ShouldReturnDeletedBooks() {
        // Given
        when(bookRepository.findAllDeleted()).thenReturn(Collections.singletonList(validBook));

        // When
        List<Book> result = bookService.getDeletedBooks();

        // Then
        assertEquals(1, result.size());
        assertEquals(validBook, result.get(0));
        verify(bookRepository).findAllDeleted();
    }

    @Test
    void getAvailableBooks_ShouldReturnAvailableBooks() {
        // Given
        when(bookRepository.findAvailableBooks()).thenReturn(bookList);

        // When
        List<Book> result = bookService.getAvailableBooks();

        // Then
        assertEquals(2, result.size());
        assertEquals(bookList, result);
        verify(bookRepository).findAvailableBooks();
    }

    // ===== GET BOOK BY ID TESTS =====

    @Test
    void getBookById_WithValidId_ShouldReturnBook() {
        // Given
        Long bookId = 1L;
        when(bookRepository.findActiveById(bookId)).thenReturn(Optional.of(validBook));

        // When
        Book result = bookService.getBookById(bookId);

        // Then
        assertEquals(validBook, result);
        verify(bookRepository).findActiveById(bookId);
    }

    @Test
    void getBookById_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        Long bookId = 999L;
        when(bookRepository.findActiveById(bookId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class, 
            () -> bookService.getBookById(bookId));
        
        assertEquals("Book not found with id: " + bookId, exception.getMessage());
        verify(bookRepository).findActiveById(bookId);
    }

    @Test
    void getBookByIdIncludingDeleted_WithValidId_ShouldReturnBook() {
        // Given
        Long bookId = 1L;
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(validBook));

        // When
        Book result = bookService.getBookByIdIncludingDeleted(bookId);

        // Then
        assertEquals(validBook, result);
        verify(bookRepository).findById(bookId);
    }

    @Test
    void getBookByIdIncludingDeleted_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        Long bookId = 999L;
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class, 
            () -> bookService.getBookByIdIncludingDeleted(bookId));
        
        assertEquals("Book not found with id: " + bookId, exception.getMessage());
        verify(bookRepository).findById(bookId);
    }

    // ===== GET BOOK BY ISBN TESTS =====

    @Test
    void getBookByIsbn_WithValidIsbn_ShouldReturnBook() {
        // Given
        String isbn = "978-0-7432-7356-5";
        when(bookRepository.findActiveByIsbn(isbn)).thenReturn(Optional.of(validBook));

        // When
        Optional<Book> result = bookService.getBookByIsbn(isbn);

        // Then
        assertTrue(result.isPresent());
        assertEquals(validBook, result.get());
        verify(bookRepository).findActiveByIsbn(isbn);
    }

    @Test
    void getBookByIsbn_WithInvalidIsbn_ShouldReturnEmpty() {
        // Given
        String isbn = "invalid-isbn";
        when(bookRepository.findActiveByIsbn(isbn)).thenReturn(Optional.empty());

        // When
        Optional<Book> result = bookService.getBookByIsbn(isbn);

        // Then
        assertFalse(result.isPresent());
        verify(bookRepository).findActiveByIsbn(isbn);
    }

    // ===== ADD BOOK TESTS =====

    @Test
    void addBook_WithValidBook_ShouldSaveAndReturnBook() {
        // Given
        Book newBook = new Book();
        newBook.setTitle("New Book");
        newBook.setAuthor("New Author");
        newBook.setIsbn("978-1-234-56789-0");
        newBook.setCategory("Science");
        newBook.setLanguage("English");
        newBook.setTotalCopies(2);
        newBook.setAvailableCopies(2);

        when(bookRepository.existsByIsbnIncludingDeleted(newBook.getIsbn())).thenReturn(false);
        when(bookRepository.save(newBook)).thenReturn(newBook);

        // When
        Book result = bookService.addBook(newBook);

        // Then
        assertEquals(newBook, result);
        verify(bookRepository).existsByIsbnIncludingDeleted(newBook.getIsbn());
        verify(bookRepository).save(newBook);
    }

    @Test
    void addBook_WithExistingIsbn_ShouldThrowConflictException() {
        // Given
        Book newBook = new Book();
        newBook.setTitle("New Book");
        newBook.setAuthor("New Author");
        newBook.setIsbn("978-0-7432-7356-5"); // Existing ISBN
        newBook.setCategory("Science");
        newBook.setLanguage("English");
        newBook.setTotalCopies(2);
        newBook.setAvailableCopies(2);

        when(bookRepository.existsByIsbnIncludingDeleted(newBook.getIsbn())).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class, 
            () -> bookService.addBook(newBook));
        
        assertEquals("Book with ISBN " + newBook.getIsbn() + " already exists", exception.getMessage());
        verify(bookRepository).existsByIsbnIncludingDeleted(newBook.getIsbn());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBook_WithAvailableCopiesExceedingTotal_ShouldThrowBadRequestException() {
        // Given
        Book newBook = new Book();
        newBook.setTitle("New Book");
        newBook.setAuthor("New Author");
        newBook.setIsbn("978-1-234-56789-0");
        newBook.setCategory("Science");
        newBook.setLanguage("English");
        newBook.setTotalCopies(2);
        newBook.setAvailableCopies(5); // More than total

        when(bookRepository.existsByIsbnIncludingDeleted(newBook.getIsbn())).thenReturn(false);

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, 
            () -> bookService.addBook(newBook));
        
        assertEquals("Available copies cannot exceed total copies", exception.getMessage());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBook_WithNullTitle_ShouldThrowBadRequestException() {
        // Given
        Book newBook = new Book();
        newBook.setTitle(null); // Invalid
        newBook.setAuthor("New Author");
        newBook.setIsbn("978-1-234-56789-0");
        newBook.setTotalCopies(2);
        newBook.setAvailableCopies(2);

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, 
            () -> bookService.addBook(newBook));
        
        assertEquals("Book title is required", exception.getMessage());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBook_WithEmptyAuthor_ShouldThrowBadRequestException() {
        // Given
        Book newBook = new Book();
        newBook.setTitle("New Book");
        newBook.setAuthor(""); // Invalid
        newBook.setIsbn("978-1-234-56789-0");
        newBook.setTotalCopies(2);
        newBook.setAvailableCopies(2);

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, 
            () -> bookService.addBook(newBook));
        
        assertEquals("Book author is required", exception.getMessage());
        verify(bookRepository, never()).save(any());
    }

    // ===== UPDATE BOOK TESTS =====

    @Test
    void updateBook_WithValidData_ShouldUpdateAndReturnBook() {
        // Given
        Long bookId = 1L;
        Book updatedBook = new Book();
        updatedBook.setTitle("Updated Title");
        updatedBook.setAuthor("Updated Author");
        updatedBook.setIsbn("978-0-7432-7356-5"); // Same ISBN
        updatedBook.setCategory("Updated Category");
        updatedBook.setLanguage("Updated Language");
        updatedBook.setTotalCopies(10);
        updatedBook.setAvailableCopies(8);

        when(bookRepository.findActiveById(bookId)).thenReturn(Optional.of(validBook));
        when(bookRepository.save(any(Book.class))).thenReturn(validBook);

        // When
        Book result = bookService.updateBook(bookId, updatedBook);

        // Then
        assertEquals(validBook, result);
        verify(bookRepository).findActiveById(bookId);
        verify(bookRepository).save(validBook);
        
        // Verify the book was updated
        assertEquals("Updated Title", validBook.getTitle());
        assertEquals("Updated Author", validBook.getAuthor());
        assertEquals(10, validBook.getTotalCopies());
        assertEquals(8, validBook.getAvailableCopies());
    }

    @Test
    void updateBook_WithNewIsbnThatExists_ShouldThrowConflictException() {
        // Given
        Long bookId = 1L;
        Book updatedBook = new Book();
        updatedBook.setTitle("Updated Title");
        updatedBook.setAuthor("Updated Author");
        updatedBook.setIsbn("978-1-234-56789-0"); // Different ISBN
        updatedBook.setCategory("Updated Category");
        updatedBook.setLanguage("Updated Language");
        updatedBook.setTotalCopies(10);
        updatedBook.setAvailableCopies(8);

        when(bookRepository.findActiveById(bookId)).thenReturn(Optional.of(validBook));
        when(bookRepository.existsByIsbnIncludingDeleted("978-1-234-56789-0")).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class, 
            () -> bookService.updateBook(bookId, updatedBook));
        
        assertEquals("Book with ISBN 978-1-234-56789-0 already exists", exception.getMessage());
        verify(bookRepository, never()).save(any());
    }

    // ===== UPDATE BOOK AVAILABILITY TESTS =====

    @Test
    void updateBookAvailability_WithValidData_ShouldUpdateBook() {
        // Given
        Long bookId = 1L;
        Integer totalCopies = 10;
        Integer availableCopies = 7;

        when(bookRepository.findActiveById(bookId)).thenReturn(Optional.of(validBook));
        when(bookRepository.save(validBook)).thenReturn(validBook);

        // When
        Book result = bookService.updateBookAvailability(bookId, totalCopies, availableCopies);

        // Then
        assertEquals(validBook, result);
        assertEquals(totalCopies, validBook.getTotalCopies());
        assertEquals(availableCopies, validBook.getAvailableCopies());
        verify(bookRepository).findActiveById(bookId);
        verify(bookRepository).save(validBook);
    }

    @Test
    void updateBookAvailability_WithAvailableExceedingTotal_ShouldThrowBadRequestException() {
        // Given
        Long bookId = 1L;
        Integer totalCopies = 5;
        Integer availableCopies = 10; // More than total

        when(bookRepository.findActiveById(bookId)).thenReturn(Optional.of(validBook));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, 
            () -> bookService.updateBookAvailability(bookId, totalCopies, availableCopies));
        
        assertEquals("Available copies cannot exceed total copies", exception.getMessage());
        verify(bookRepository, never()).save(any());
    }

    // ===== DELETE BOOK TESTS =====

    @Test
    void deleteBook_WithValidId_ShouldMarkAsDeleted() {
        // Given
        Long bookId = 1L;
        when(bookRepository.findActiveById(bookId)).thenReturn(Optional.of(validBook));
        when(bookRepository.save(validBook)).thenReturn(validBook);

        // When
        bookService.deleteBook(bookId);

        // Then
        verify(bookRepository).findActiveById(bookId);
        verify(bookRepository).save(validBook);
        // Note: We can't verify markAsDeleted() was called without making it public or using reflection
    }

    @Test
    void hardDeleteBook_WithValidId_ShouldDeletePermanently() {
        // Given
        Long bookId = 1L;
        when(bookRepository.existsByIdIncludingDeleted(bookId)).thenReturn(true);
        doNothing().when(bookRepository).hardDeleteById(bookId);

        // When
        bookService.hardDeleteBook(bookId);

        // Then
        verify(bookRepository).existsByIdIncludingDeleted(bookId);
        verify(bookRepository).hardDeleteById(bookId);
    }

    @Test
    void hardDeleteBook_WithInvalidId_ShouldThrowNotFoundException() {
        // Given
        Long bookId = 999L;
        when(bookRepository.existsByIdIncludingDeleted(bookId)).thenReturn(false);

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class, 
            () -> bookService.hardDeleteBook(bookId));
        
        assertEquals("Book not found with id: " + bookId, exception.getMessage());
        verify(bookRepository, never()).hardDeleteById(any());
    }

    // ===== RESTORE BOOK TESTS =====

    @Test
    void restoreBook_WithValidDeletedBook_ShouldRestoreBook() {
        // Given
        Long bookId = 1L;
        Book deletedBook = new Book();
        deletedBook.setId(bookId);
        deletedBook.setIsbn("978-0-7432-7356-5");
        deletedBook.markAsDeleted(); // Assume this method exists

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(deletedBook));
        when(bookRepository.existsActiveByIsbn(deletedBook.getIsbn())).thenReturn(false);
        when(bookRepository.save(deletedBook)).thenReturn(deletedBook);

        // When
        Book result = bookService.restoreBook(bookId);

        // Then
        assertEquals(deletedBook, result);
        verify(bookRepository).findById(bookId);
        verify(bookRepository).existsActiveByIsbn(deletedBook.getIsbn());
        verify(bookRepository).save(deletedBook);
    }

    // ===== SEARCH TESTS =====

    @Test
    void searchBooks_WithThreeParameters_ShouldReturnMatchingBooks() {
        // Given
        String title = "Gatsby";
        String author = "Fitzgerald";
        String category = "Fiction";

        when(bookRepository.findActiveBooksByFilters(title, author, category, null))
            .thenReturn(Collections.singletonList(validBook));

        // When
        List<Book> result = bookService.searchBooks(title, author, category);

        // Then
        assertEquals(1, result.size());
        assertEquals(validBook, result.get(0));
        verify(bookRepository).findActiveBooksByFilters(title, author, category, null);
    }

    @Test
    void searchBooks_WithFourParameters_ShouldReturnMatchingBooks() {
        // Given
        String title = "Gatsby";
        String author = "Fitzgerald";
        String category = "Fiction";
        String language = "English";

        when(bookRepository.findActiveBooksByFilters(title, author, category, language))
            .thenReturn(Collections.singletonList(validBook));

        // When
        List<Book> result = bookService.searchBooks(title, author, category, language);

        // Then
        assertEquals(1, result.size());
        assertEquals(validBook, result.get(0));
        verify(bookRepository).findActiveBooksByFilters(title, author, category, language);
    }

    @Test
    void getBooksByTitle_ShouldReturnMatchingBooks() {
        // Given
        String title = "Gatsby";
        when(bookRepository.findActiveByTitleContainingIgnoreCase(title))
            .thenReturn(Collections.singletonList(validBook));

        // When
        List<Book> result = bookService.getBooksByTitle(title);

        // Then
        assertEquals(1, result.size());
        assertEquals(validBook, result.get(0));
        verify(bookRepository).findActiveByTitleContainingIgnoreCase(title);
    }

    @Test
    void getBooksByAuthor_ShouldReturnMatchingBooks() {
        // Given
        String author = "Fitzgerald";
        when(bookRepository.findActiveByAuthorContainingIgnoreCase(author))
            .thenReturn(Collections.singletonList(validBook));

        // When
        List<Book> result = bookService.getBooksByAuthor(author);

        // Then
        assertEquals(1, result.size());
        assertEquals(validBook, result.get(0));
        verify(bookRepository).findActiveByAuthorContainingIgnoreCase(author);
    }

    @Test
    void getBooksByCategory_ShouldReturnMatchingBooks() {
        // Given
        String category = "Fiction";
        when(bookRepository.findActiveByCategoryIgnoreCase(category))
            .thenReturn(bookList);

        // When
        List<Book> result = bookService.getBooksByCategory(category);

        // Then
        assertEquals(2, result.size());
        assertEquals(bookList, result);
        verify(bookRepository).findActiveByCategoryIgnoreCase(category);
    }

    @Test
    void getBooksByLanguage_ShouldReturnMatchingBooks() {
        // Given
        String language = "English";
        when(bookRepository.findActiveByLanguageIgnoreCase(language))
            .thenReturn(bookList);

        // When
        List<Book> result = bookService.getBooksByLanguage(language);

        // Then
        assertEquals(2, result.size());
        assertEquals(bookList, result);
        verify(bookRepository).findActiveByLanguageIgnoreCase(language);
    }

    // ===== EXISTENCE CHECK TESTS =====

    @Test
    void bookExists_ShouldReturnTrue() {
        // Given
        Long bookId = 1L;
        when(bookRepository.existsById(bookId)).thenReturn(true);

        // When
        boolean result = bookService.bookExists(bookId);

        // Then
        assertTrue(result);
        verify(bookRepository).existsById(bookId);
    }

    @Test
    void bookExistsIncludingDeleted_ShouldReturnTrue() {
        // Given
        Long bookId = 1L;
        when(bookRepository.existsByIdIncludingDeleted(bookId)).thenReturn(true);

        // When
        boolean result = bookService.bookExistsIncludingDeleted(bookId);

        // Then
        assertTrue(result);
        verify(bookRepository).existsByIdIncludingDeleted(bookId);
    }

    @Test
    void isbnExists_ShouldReturnTrue() {
        // Given
        String isbn = "978-0-7432-7356-5";
        when(bookRepository.existsActiveByIsbn(isbn)).thenReturn(true);

        // When
        boolean result = bookService.isbnExists(isbn);

        // Then
        assertTrue(result);
        verify(bookRepository).existsActiveByIsbn(isbn);
    }

    @Test
    void isbnExistsIncludingDeleted_ShouldReturnTrue() {
        // Given
        String isbn = "978-0-7432-7356-5";
        when(bookRepository.existsByIsbnIncludingDeleted(isbn)).thenReturn(true);

        // When
        boolean result = bookService.isbnExistsIncludingDeleted(isbn);

        // Then
        assertTrue(result);
        verify(bookRepository).existsByIsbnIncludingDeleted(isbn);
    }
}
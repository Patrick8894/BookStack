package com.bookstack.bookstack.book.service;

import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.repository.BookRepository;
import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.ConflictException;
import com.bookstack.bookstack.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAllActive();
    }

    public List<Book> getAllBooksIncludingDeleted() {
        return bookRepository.findAll();
    }

    public List<Book> getDeletedBooks() {
        return bookRepository.findAllDeleted();
    }

    public List<Book> getAvailableBooks() {
        return bookRepository.findAvailableBooks();
    }

    public Book getBookById(Long id) {
        return bookRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
    }

    public Book getBookByIdIncludingDeleted(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
    }

    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findActiveByIsbn(isbn);
    }

    public Book addBook(Book book) {
        // Validate required fields
        validateBookData(book);
        
        // Check if ISBN already exists (including deleted books)
        if (bookRepository.existsByIsbnIncludingDeleted(book.getIsbn())) {
            throw new ConflictException("Book with ISBN " + book.getIsbn() + " already exists");
        }
        
        // Ensure available copies doesn't exceed total copies
        if (book.getAvailableCopies() > book.getTotalCopies()) {
            throw new BadRequestException("Available copies cannot exceed total copies");
        }
        
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book updatedBook) {
        Book book = getBookById(id);
        
        // Validate required fields
        validateBookData(updatedBook);
        
        // Check if ISBN is being changed and if new ISBN is taken
        if (!book.getIsbn().equals(updatedBook.getIsbn()) && 
            bookRepository.existsByIsbnIncludingDeleted(updatedBook.getIsbn())) {
            throw new ConflictException("Book with ISBN " + updatedBook.getIsbn() + " already exists");
        }
        
        // Ensure available copies doesn't exceed total copies
        if (updatedBook.getAvailableCopies() > updatedBook.getTotalCopies()) {
            throw new BadRequestException("Available copies cannot exceed total copies");
        }
        
        // Update book fields
        book.setTitle(updatedBook.getTitle());
        book.setAuthor(updatedBook.getAuthor());
        book.setIsbn(updatedBook.getIsbn());
        book.setCategory(updatedBook.getCategory());
        book.setLanguage(updatedBook.getLanguage());
        book.setTotalCopies(updatedBook.getTotalCopies());
        book.setAvailableCopies(updatedBook.getAvailableCopies());
        
        return bookRepository.save(book);
    }

    public Book updateBookAvailability(Long id, Integer totalCopies, Integer availableCopies) {
        Book book = getBookById(id);
        
        if (availableCopies > totalCopies) {
            throw new BadRequestException("Available copies cannot exceed total copies");
        }
        
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(availableCopies);
        return bookRepository.save(book);
    }

    // Soft delete
    public void deleteBook(Long id) {
        Book book = getBookById(id);
        book.markAsDeleted();
        bookRepository.save(book);
    }

    // Hard delete (physical deletion) - for admin use only
    public void hardDeleteBook(Long id) {
        if (!bookRepository.existsByIdIncludingDeleted(id)) {
            throw new NotFoundException("Book not found with id: " + id);
        }
        bookRepository.hardDeleteById(id);
    }

    // Restore soft-deleted book
    public Book restoreBook(Long id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
        
        if (!book.isDeleted()) {
            throw new BadRequestException("Book is not deleted");
        }
        
        // Check if ISBN is still unique among active books
        if (bookRepository.existsActiveByIsbn(book.getIsbn())) {
            throw new ConflictException("Cannot restore book: ISBN is already taken by another active book");
        }
        
        book.restore();
        return bookRepository.save(book);
    }

    public List<Book> searchBooks(String title, String author, String category) {
        return bookRepository.findActiveBooksByFilters(title, author, category, null);
    }

    public List<Book> searchBooks(String title, String author, String category, String language) {
        return bookRepository.findActiveBooksByFilters(title, author, category, language);
    }

    public List<Book> getBooksByTitle(String title) {
        return bookRepository.findActiveByTitleContainingIgnoreCase(title);
    }

    public List<Book> getBooksByAuthor(String author) {
        return bookRepository.findActiveByAuthorContainingIgnoreCase(author);
    }

    public List<Book> getBooksByCategory(String category) {
        return bookRepository.findActiveByCategoryIgnoreCase(category);
    }

    public List<Book> getBooksByLanguage(String language) {
        return bookRepository.findActiveByLanguageIgnoreCase(language);
    }

    public boolean bookExists(Long id) {
        return bookRepository.existsById(id); // This will use the @SQLRestriction clause
    }

    public boolean bookExistsIncludingDeleted(Long id) {
        return bookRepository.existsByIdIncludingDeleted(id);
    }

    public boolean isbnExists(String isbn) {
        return bookRepository.existsActiveByIsbn(isbn);
    }

    public boolean isbnExistsIncludingDeleted(String isbn) {
        return bookRepository.existsByIsbnIncludingDeleted(isbn);
    }

    private void validateBookData(Book book) {
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Book title is required");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new BadRequestException("Book author is required");
        }
        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw new BadRequestException("Book ISBN is required");
        }
        if (book.getTotalCopies() == null || book.getTotalCopies() < 0) {
            throw new BadRequestException("Total copies must be a non-negative number");
        }
        if (book.getAvailableCopies() == null || book.getAvailableCopies() < 0) {
            throw new BadRequestException("Available copies must be a non-negative number");
        }
    }
}
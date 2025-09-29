package com.bookstack.bookstack.book.controller;

import com.bookstack.bookstack.auth.annotation.RequireRole;
import com.bookstack.bookstack.book.dto.BookInput;
import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.service.BookService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import jakarta.validation.Valid;

@Controller
public class BookGraphQLController {
    private final BookService bookService;

    public BookGraphQLController(BookService bookService) {
        this.bookService = bookService;
    }

    // Queries - Allow all authenticated users to view active books
    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public List<Book> allBooks() {
        return bookService.getAllBooks();
    }

    @QueryMapping
    @RequireRole({"ADMIN"})
    public List<Book> allBooksIncludingDeleted() {
        return bookService.getAllBooksIncludingDeleted();
    }

    @QueryMapping
    @RequireRole({"ADMIN"})
    public List<Book> deletedBooks() {
        return bookService.getDeletedBooks();
    }

    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public List<Book> availableBooks() {
        return bookService.getAvailableBooks();
    }

    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public Book bookById(@Argument Long id) {
        return bookService.getBookById(id);
    }

    @QueryMapping
    @RequireRole({"ADMIN"})
    public Book bookByIdIncludingDeleted(@Argument Long id) {
        return bookService.getBookByIdIncludingDeleted(id);
    }

    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public Book bookByIsbn(@Argument String isbn) {
        return bookService.getBookByIsbn(isbn).orElse(null);
    }

    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public List<Book> searchBooks(@Argument String title, @Argument String author, 
                                 @Argument String category, @Argument String language) {
        return bookService.searchBooks(title, author, category, language);
    }

    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public List<Book> booksByTitle(@Argument String title) {
        return bookService.getBooksByTitle(title);
    }

    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public List<Book> booksByAuthor(@Argument String author) {
        return bookService.getBooksByAuthor(author);
    }

    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public List<Book> booksByCategory(@Argument String category) {
        return bookService.getBooksByCategory(category);
    }

    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public List<Book> booksByLanguage(@Argument String language) {
        return bookService.getBooksByLanguage(language);
    }

    // Mutations - Only librarians and admins can modify book data
    @MutationMapping
    @RequireRole({"LIBRARIAN", "ADMIN"})
    public Book addBook(@Argument @Valid BookInput input) {
        Book book = new Book();
        book.setTitle(input.getTitle());
        book.setAuthor(input.getAuthor());
        book.setIsbn(input.getIsbn());
        book.setCategory(input.getCategory());
        book.setLanguage(input.getLanguage());
        book.setTotalCopies(input.getTotalCopies());
        book.setAvailableCopies(input.getAvailableCopies());
        return bookService.addBook(book);
    }

    @MutationMapping
    @RequireRole({"LIBRARIAN", "ADMIN"})
    public Book updateBook(@Argument Long id, @Argument @Valid BookInput input) {
        Book updated = new Book();
        updated.setTitle(input.getTitle());
        updated.setAuthor(input.getAuthor());
        updated.setIsbn(input.getIsbn());
        updated.setCategory(input.getCategory());
        updated.setLanguage(input.getLanguage());
        updated.setTotalCopies(input.getTotalCopies());
        updated.setAvailableCopies(input.getAvailableCopies());
        return bookService.updateBook(id, updated);
    }

    @MutationMapping
    @RequireRole({"LIBRARIAN", "ADMIN"})
    public Book updateBookAvailability(@Argument Long id, @Argument Integer totalCopies, 
                                      @Argument Integer availableCopies) {
        return bookService.updateBookAvailability(id, totalCopies, availableCopies);
    }

    @MutationMapping
    @RequireRole({"ADMIN"})
    public Boolean deleteBook(@Argument Long id) {
        bookService.deleteBook(id);
        return true;
    }

    @MutationMapping
    @RequireRole({"ADMIN"})
    public Boolean hardDeleteBook(@Argument Long id) {
        bookService.hardDeleteBook(id);
        return true;
    }

    @MutationMapping
    @RequireRole({"ADMIN"})
    public Book restoreBook(@Argument Long id) {
        return bookService.restoreBook(id);
    }
}
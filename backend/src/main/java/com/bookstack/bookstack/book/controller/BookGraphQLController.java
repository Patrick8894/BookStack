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

    // Queries - Allow all authenticated users to view books
    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public List<Book> allBooks() {
        return bookService.getAllBooks();
    }

    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public Book bookById(@Argument Long id) {
        return bookService.getBookById(id);
    }

    @QueryMapping
    @RequireRole({"MEMBER", "LIBRARIAN", "ADMIN"})
    public List<Book> searchBooks(@Argument String title, @Argument String author, @Argument String category) {
        return bookService.searchBooks(title, author, category);
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
    public Book updateBook(@Argument Long id, @Argument BookInput input) {
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
    @RequireRole({"ADMIN"})
    public Boolean deleteBook(@Argument Long id) {
        bookService.deleteBook(id);
        return true;
    }
}
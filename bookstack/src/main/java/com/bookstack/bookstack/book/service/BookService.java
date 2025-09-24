package com.bookstack.bookstack.book.service;

import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.repository.BookRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

import com.bookstack.bookstack.auth.repository.UserRepository;
import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.ConflictException;
import com.bookstack.bookstack.common.exception.NotFoundException;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    public Book addBook(Book book) {
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book updatedBook) {
        return bookRepository.findById(id)
            .map(book -> {
                book.setTitle(updatedBook.getTitle());
                book.setAuthor(updatedBook.getAuthor());
                book.setIsbn(updatedBook.getIsbn());
                book.setCategory(updatedBook.getCategory());
                book.setLanguage(updatedBook.getLanguage());
                book.setTotalCopies(updatedBook.getTotalCopies());
                book.setAvailableCopies(updatedBook.getAvailableCopies());
                return bookRepository.save(book);
            })
            .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    public List<Book> searchBooks(String title, String author, String category) {
        if (title != null) return bookRepository.findByTitleContainingIgnoreCase(title);
        if (author != null) return bookRepository.findByAuthorContainingIgnoreCase(author);
        if (category != null) return bookRepository.findByCategoryIgnoreCase(category);
        return bookRepository.findAll();
    }
}


package com.bookstack.bookstack.borrow.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.service.BookService;
import com.bookstack.bookstack.borrow.model.Borrow;
import com.bookstack.bookstack.borrow.model.BorrowStatus;
import com.bookstack.bookstack.borrow.repository.BorrowRepository;
import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.NotFoundException;
import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.service.UserService;

@Service
public class BorrowService {
    private final BorrowRepository borrowRepository;
    private final UserService userService;
    private final BookService bookService;
    
    private static final int DEFAULT_BORROW_DAYS = 14;

    public BorrowService(BorrowRepository borrowRepository, UserService userService, BookService bookService) {
        this.borrowRepository = borrowRepository;
        this.userService = userService;
        this.bookService = bookService;
    }

    public Borrow borrowBook(Long userId, Long bookId, String notes) {
        User user = userService.getUserById(userId);
        Book book = bookService.getBookById(bookId)
            .orElseThrow(() -> new NotFoundException("Book not found with id: " + bookId));
        
        // Check if book is available
        if (book.getAvailableCopies() <= 0) {
            throw new BadRequestException("Book is not available for borrowing");
        }
        
        // Check if user already has this book borrowed
        List<Borrow> activeBorrows = borrowRepository.findActiveBorrowByUserAndBook(userId, bookId);
        if (!activeBorrows.isEmpty()) {
            throw new BadRequestException("User already has this book borrowed");
        }
        
        LocalDateTime borrowDate = LocalDateTime.now();
        LocalDateTime dueDate = borrowDate.plusDays(DEFAULT_BORROW_DAYS);
        
        Borrow borrow = new Borrow(user, book, borrowDate, dueDate);
        borrow.setNotes(notes);
        
        // Update book availability
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookService.updateBook(bookId, book);
        
        return borrowRepository.save(borrow);
    }

    public Borrow returnBook(Long borrowId, String notes) {
        Borrow borrow = getBorrowById(borrowId);
        
        if (borrow.getStatus() != BorrowStatus.ACTIVE) {
            throw new BadRequestException("Book is already returned");
        }
        
        borrow.setStatus(BorrowStatus.RETURNED);
        borrow.setReturnDate(LocalDateTime.now());
        if (notes != null) {
            borrow.setNotes(borrow.getNotes() + " | Return notes: " + notes);
        }
        
        // Update book availability
        Book book = borrow.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookService.updateBook(book.getId(), book);
        
        return borrowRepository.save(borrow);
    }

    public List<Borrow> getAllBorrows() {
        return borrowRepository.findAll();
    }

    public Borrow getBorrowById(Long id) {
        return borrowRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Borrow record not found with id: " + id));
    }

    public List<Borrow> getBorrowsByUserId(Long userId) {
        return borrowRepository.findByUserId(userId);
    }

    public List<Borrow> getBorrowsByBookId(Long bookId) {
        return borrowRepository.findByBookId(bookId);
    }

    public List<Borrow> getBorrowsByStatus(BorrowStatus status) {
        return borrowRepository.findByStatus(status);
    }

    public List<Borrow> getActiveBorrowsByUserId(Long userId) {
        return borrowRepository.findByUserIdAndStatus(userId, BorrowStatus.ACTIVE);
    }

    public List<Borrow> getOverdueBorrows() {
        List<Borrow> overdueBorrows = borrowRepository.findOverdueBorrows(LocalDateTime.now());
        // Update status to OVERDUE
        for (Borrow borrow : overdueBorrows) {
            if (borrow.getStatus() == BorrowStatus.ACTIVE) {
                borrow.setStatus(BorrowStatus.OVERDUE);
                borrowRepository.save(borrow);
            }
        }
        return overdueBorrows;
    }

    public void deleteBorrow(Long id) {
        Borrow borrow = getBorrowById(id);
        
        // If deleting an active borrow, restore book availability
        if (borrow.getStatus() == BorrowStatus.ACTIVE) {
            Book book = borrow.getBook();
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookService.updateBook(book.getId(), book);
        }
        
        borrowRepository.deleteById(id);
    }
}
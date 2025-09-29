package com.bookstack.bookstack.borrow.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookstack.bookstack.book.model.Book;
import com.bookstack.bookstack.book.service.BookService;
import com.bookstack.bookstack.borrow.dto.BorrowResponse;
import com.bookstack.bookstack.borrow.mapper.BorrowMapper;
import com.bookstack.bookstack.borrow.model.Borrow;
import com.bookstack.bookstack.borrow.model.BorrowStatus;
import com.bookstack.bookstack.borrow.repository.BorrowRepository;
import com.bookstack.bookstack.common.exception.BadRequestException;
import com.bookstack.bookstack.common.exception.NotFoundException;
import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.service.UserService;

@Service
@Transactional
public class BorrowService {
    private final BorrowRepository borrowRepository;
    private final UserService userService;
    private final BookService bookService;
    private final BorrowMapper borrowMapper;
    
    private static final int DEFAULT_BORROW_DAYS = 14;

    public BorrowService(BorrowRepository borrowRepository, UserService userService, 
                        BookService bookService, BorrowMapper borrowMapper) {
        this.borrowRepository = borrowRepository;
        this.userService = userService;
        this.bookService = bookService;
        this.borrowMapper = borrowMapper;
    }

    public BorrowResponse borrowBook(Long userId, Long bookId, String notes) {
        User user = userService.getUserById(userId);
        Book book = bookService.getBookById(bookId);
        
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
        
        Borrow savedBorrow = borrowRepository.save(borrow);
        return borrowMapper.toResponse(savedBorrow);
    }

    public BorrowResponse returnBook(Long borrowId, String notes) {
        Borrow borrow = getBorrowEntityById(borrowId);
        
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
        
        Borrow savedBorrow = borrowRepository.save(borrow);
        return borrowMapper.toResponse(savedBorrow);
    }

    public List<BorrowResponse> getAllBorrows() {
        return borrowRepository.findAllWithUserAndBook()
                .stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());
    }

    public BorrowResponse getBorrowById(Long id) {
        Borrow borrow = getBorrowEntityById(id);
        return borrowMapper.toResponse(borrow);
    }
    
    private Borrow getBorrowEntityById(Long id) {
        return borrowRepository.findByIdWithUserAndBook(id)
            .orElseThrow(() -> new NotFoundException("Borrow record not found with id: " + id));
    }

    public List<BorrowResponse> getBorrowsByUserId(Long userId) {
        return borrowRepository.findByUserIdWithUserAndBook(userId)
                .stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<BorrowResponse> getBorrowsByBookId(Long bookId) {
        return borrowRepository.findByBookIdWithUserAndBook(bookId)
                .stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<BorrowResponse> getBorrowsByStatus(BorrowStatus status) {
        return borrowRepository.findByStatusWithUserAndBook(status)
                .stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<BorrowResponse> getActiveBorrowsByUserId(Long userId) {
        return borrowRepository.findByUserIdAndStatusWithUserAndBook(userId, BorrowStatus.ACTIVE)
                .stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<BorrowResponse> getOverdueBorrows() {
        List<Borrow> overdueBorrows = borrowRepository.findOverdueBorrowsWithUserAndBook(LocalDateTime.now());
        // Update status to OVERDUE
        for (Borrow borrow : overdueBorrows) {
            if (borrow.getStatus() == BorrowStatus.ACTIVE) {
                borrow.setStatus(BorrowStatus.OVERDUE);
                borrowRepository.save(borrow);
            }
        }
        return overdueBorrows.stream()
                .map(borrowMapper::toResponse)
                .collect(Collectors.toList());
    }

    public void deleteBorrow(Long id) {
        Borrow borrow = getBorrowEntityById(id);
        
        // If deleting an active borrow, restore book availability
        if (borrow.getStatus() == BorrowStatus.ACTIVE) {
            Book book = borrow.getBook();
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookService.updateBook(book.getId(), book);
        }
        
        borrowRepository.deleteById(id);
    }
}
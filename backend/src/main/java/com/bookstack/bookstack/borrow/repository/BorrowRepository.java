package com.bookstack.bookstack.borrow.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookstack.bookstack.borrow.model.Borrow;
import com.bookstack.bookstack.borrow.model.BorrowStatus;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Long> {
    
    // Existing methods for simple operations
    List<Borrow> findByUserId(Long userId);
    List<Borrow> findByBookId(Long bookId);
    List<Borrow> findByStatus(BorrowStatus status);
    List<Borrow> findByUserIdAndStatus(Long userId, BorrowStatus status);
    List<Borrow> findByBookIdAndStatus(Long bookId, BorrowStatus status);
    
    @Query("SELECT COUNT(b) FROM Borrow b WHERE b.book.id = :bookId AND b.status = 'ACTIVE'")
    long countActiveBorrowsByBookId(@Param("bookId") Long bookId);
    
    @Query("SELECT b FROM Borrow b WHERE b.user.id = :userId AND b.book.id = :bookId AND b.status = 'ACTIVE'")
    List<Borrow> findActiveBorrowByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    
    // New methods with eager fetching for DTOs
    @Query("SELECT b FROM Borrow b JOIN FETCH b.user JOIN FETCH b.book")
    List<Borrow> findAllWithUserAndBook();
    
    @Query("SELECT b FROM Borrow b JOIN FETCH b.user JOIN FETCH b.book WHERE b.id = :id")
    Optional<Borrow> findByIdWithUserAndBook(@Param("id") Long id);
    
    @Query("SELECT b FROM Borrow b JOIN FETCH b.user JOIN FETCH b.book WHERE b.user.id = :userId")
    List<Borrow> findByUserIdWithUserAndBook(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Borrow b JOIN FETCH b.user JOIN FETCH b.book WHERE b.book.id = :bookId")
    List<Borrow> findByBookIdWithUserAndBook(@Param("bookId") Long bookId);
    
    @Query("SELECT b FROM Borrow b JOIN FETCH b.user JOIN FETCH b.book WHERE b.status = :status")
    List<Borrow> findByStatusWithUserAndBook(@Param("status") BorrowStatus status);
    
    @Query("SELECT b FROM Borrow b JOIN FETCH b.user JOIN FETCH b.book WHERE b.user.id = :userId AND b.status = :status")
    List<Borrow> findByUserIdAndStatusWithUserAndBook(@Param("userId") Long userId, @Param("status") BorrowStatus status);
    
    @Query("SELECT b FROM Borrow b JOIN FETCH b.user JOIN FETCH b.book WHERE b.dueDate < :currentDate AND b.status = 'ACTIVE'")
    List<Borrow> findOverdueBorrowsWithUserAndBook(@Param("currentDate") LocalDateTime currentDate);
}
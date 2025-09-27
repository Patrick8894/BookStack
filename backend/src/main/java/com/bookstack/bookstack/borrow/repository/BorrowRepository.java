package com.bookstack.bookstack.borrow.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookstack.bookstack.borrow.model.Borrow;
import com.bookstack.bookstack.borrow.model.BorrowStatus;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Long> {
    List<Borrow> findByUserId(Long userId);
    List<Borrow> findByBookId(Long bookId);
    List<Borrow> findByStatus(BorrowStatus status);
    List<Borrow> findByUserIdAndStatus(Long userId, BorrowStatus status);
    List<Borrow> findByBookIdAndStatus(Long bookId, BorrowStatus status);
    
    @Query("SELECT b FROM Borrow b WHERE b.dueDate < :currentDate AND b.status = 'ACTIVE'")
    List<Borrow> findOverdueBorrows(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT COUNT(b) FROM Borrow b WHERE b.book.id = :bookId AND b.status = 'ACTIVE'")
    long countActiveBorrowsByBookId(@Param("bookId") Long bookId);
    
    @Query("SELECT b FROM Borrow b WHERE b.user.id = :userId AND b.book.id = :bookId AND b.status = 'ACTIVE'")
    List<Borrow> findActiveBorrowByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
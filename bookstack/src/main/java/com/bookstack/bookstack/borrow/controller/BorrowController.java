package com.bookstack.bookstack.borrow.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookstack.bookstack.borrow.dto.BorrowRequest;
import com.bookstack.bookstack.borrow.dto.ReturnRequest;
import com.bookstack.bookstack.borrow.model.Borrow;
import com.bookstack.bookstack.borrow.model.BorrowStatus;
import com.bookstack.bookstack.borrow.service.BorrowService;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {
    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    public ResponseEntity<Borrow> borrowBook(@Valid @RequestBody BorrowRequest request) {
        Borrow borrow = borrowService.borrowBook(request.getUserId(), request.getBookId(), request.getNotes());
        return ResponseEntity.status(HttpStatus.CREATED).body(borrow);
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<Borrow> returnBook(@PathVariable Long id, @RequestBody ReturnRequest request) {
        Borrow borrow = borrowService.returnBook(id, request.getNotes());
        return ResponseEntity.ok(borrow);
    }

    @GetMapping
    public ResponseEntity<List<Borrow>> getAllBorrows() {
        List<Borrow> borrows = borrowService.getAllBorrows();
        return ResponseEntity.ok(borrows);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Borrow> getBorrowById(@PathVariable Long id) {
        Borrow borrow = borrowService.getBorrowById(id);
        return ResponseEntity.ok(borrow);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Borrow>> getBorrowsByUser(@PathVariable Long userId) {
        List<Borrow> borrows = borrowService.getBorrowsByUserId(userId);
        return ResponseEntity.ok(borrows);
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Borrow>> getActiveBorrowsByUser(@PathVariable Long userId) {
        List<Borrow> borrows = borrowService.getActiveBorrowsByUserId(userId);
        return ResponseEntity.ok(borrows);
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<Borrow>> getBorrowsByBook(@PathVariable Long bookId) {
        List<Borrow> borrows = borrowService.getBorrowsByBookId(bookId);
        return ResponseEntity.ok(borrows);
    }

    @GetMapping("/status")
    public ResponseEntity<List<Borrow>> getBorrowsByStatus(@RequestParam BorrowStatus status) {
        List<Borrow> borrows = borrowService.getBorrowsByStatus(status);
        return ResponseEntity.ok(borrows);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Borrow>> getOverdueBorrows() {
        List<Borrow> borrows = borrowService.getOverdueBorrows();
        return ResponseEntity.ok(borrows);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBorrow(@PathVariable Long id) {
        borrowService.deleteBorrow(id);
        return ResponseEntity.ok(Map.of("message", "Borrow record deleted successfully"));
    }
}
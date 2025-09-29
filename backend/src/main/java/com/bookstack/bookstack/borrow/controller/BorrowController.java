package com.bookstack.bookstack.borrow.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
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

import com.bookstack.bookstack.auth.annotation.RequireRole;
import com.bookstack.bookstack.auth.service.JwtService;
import com.bookstack.bookstack.borrow.dto.BorrowRequest;
import com.bookstack.bookstack.borrow.dto.BorrowResponse;
import com.bookstack.bookstack.borrow.dto.ReturnRequest;
import com.bookstack.bookstack.borrow.model.BorrowStatus;
import com.bookstack.bookstack.borrow.service.BorrowService;
import com.bookstack.bookstack.common.exception.ForbiddenException;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {
    private final BorrowService borrowService;
    private final JwtService jwtService;

    public BorrowController(BorrowService borrowService, JwtService jwtService) {
        this.borrowService = borrowService;
        this.jwtService = jwtService;
    }

    @PostMapping
    @RequireRole({"LIBRARIAN", "ADMIN"})
    public ResponseEntity<BorrowResponse> borrowBook(@Valid @RequestBody BorrowRequest request) {
        BorrowResponse borrow = borrowService.borrowBook(request.getUserId(), request.getBookId(), request.getNotes());
        return ResponseEntity.status(HttpStatus.CREATED).body(borrow);
    }

    @PutMapping("/{id}/return")
    @RequireRole({"LIBRARIAN", "ADMIN"})
    public ResponseEntity<BorrowResponse> returnBook(@PathVariable Long id, @RequestBody ReturnRequest request) {
        BorrowResponse borrow = borrowService.returnBook(id, request.getNotes());
        return ResponseEntity.ok(borrow);
    }

    @GetMapping
    @RequireRole({"ADMIN"})
    public ResponseEntity<List<BorrowResponse>> getAllBorrows() {
        List<BorrowResponse> borrows = borrowService.getAllBorrows();
        return ResponseEntity.ok(borrows);
    }

    @GetMapping("/{id}")
    @RequireRole({"LIBRARIAN", "ADMIN"})
    public ResponseEntity<BorrowResponse> getBorrowById(@PathVariable Long id) {
        BorrowResponse borrow = borrowService.getBorrowById(id);
        return ResponseEntity.ok(borrow);
    }

    @GetMapping("/user/{userId}")
    @RequireRole({"LIBRARIAN", "ADMIN", "MEMBER"})
    public ResponseEntity<List<BorrowResponse>> getBorrowsByUser(
            @PathVariable Long userId,
            HttpServletRequest request) {
        
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);
        String role = jwtService.extractRole(token);
        Long tokenUserId = jwtService.extractUserId(token);
        
        // If user is a MEMBER, verify they're only accessing their own records
        if ("MEMBER".equals(role) && !userId.equals(tokenUserId)) {
            throw new ForbiddenException("Members can only access their own borrow records");
        }
        
        List<BorrowResponse> borrows = borrowService.getBorrowsByUserId(userId);
        return ResponseEntity.ok(borrows);
    }

    @GetMapping("/user/{userId}/active")
    @RequireRole({"LIBRARIAN", "ADMIN", "MEMBER"})
    public ResponseEntity<List<BorrowResponse>> getActiveBorrowsByUser(
            @PathVariable Long userId,
            HttpServletRequest request) {
        
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);
        String role = jwtService.extractRole(token);
        Long tokenUserId = jwtService.extractUserId(token);
        
        // If user is a MEMBER, verify they're only accessing their own records
        if ("MEMBER".equals(role) && !userId.equals(tokenUserId)) {
            throw new ForbiddenException("Members can only access their own borrow records");
        }
        
        List<BorrowResponse> borrows = borrowService.getActiveBorrowsByUserId(userId);
        return ResponseEntity.ok(borrows);
    }

    @GetMapping("/book/{bookId}")
    @RequireRole({"LIBRARIAN", "ADMIN"})
    public ResponseEntity<List<BorrowResponse>> getBorrowsByBook(@PathVariable Long bookId) {
        List<BorrowResponse> borrows = borrowService.getBorrowsByBookId(bookId);
        return ResponseEntity.ok(borrows);
    }

    @GetMapping("/status")
    @RequireRole({"LIBRARIAN", "ADMIN"})
    public ResponseEntity<List<BorrowResponse>> getBorrowsByStatus(@RequestParam BorrowStatus status) {
        List<BorrowResponse> borrows = borrowService.getBorrowsByStatus(status);
        return ResponseEntity.ok(borrows);
    }

    @GetMapping("/overdue")
    @RequireRole({"LIBRARIAN", "ADMIN"})
    public ResponseEntity<List<BorrowResponse>> getOverdueBorrows() {
        List<BorrowResponse> borrows = borrowService.getOverdueBorrows();
        return ResponseEntity.ok(borrows);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public ResponseEntity<?> deleteBorrow(@PathVariable Long id) {
        borrowService.deleteBorrow(id);
        return ResponseEntity.ok(Map.of("message", "Borrow record deleted successfully"));
    }
}
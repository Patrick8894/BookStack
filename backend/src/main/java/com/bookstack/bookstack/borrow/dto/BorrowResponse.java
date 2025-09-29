package com.bookstack.bookstack.borrow.dto;

import java.time.LocalDateTime;

import com.bookstack.bookstack.borrow.model.BorrowStatus;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BorrowResponse {
    private Long id;
    private Long userId;
    private Long bookId;
    private String userName;
    private String bookTitle;
    private BorrowStatus status;
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private String notes;
}
package com.bookstack.bookstack.borrow.mapper;

import org.springframework.stereotype.Component;

import com.bookstack.bookstack.borrow.dto.BorrowResponse;
import com.bookstack.bookstack.borrow.model.Borrow;

@Component
public class BorrowMapper {
    
    public BorrowResponse toResponse(Borrow borrow) {
        BorrowResponse response = new BorrowResponse();
        response.setId(borrow.getId());
        response.setUserId(borrow.getUser().getId());
        response.setBookId(borrow.getBook().getId());
        response.setUserName(borrow.getUser().getUsername());
        response.setBookTitle(borrow.getBook().getTitle());
        response.setStatus(borrow.getStatus());
        response.setBorrowDate(borrow.getBorrowDate());
        response.setDueDate(borrow.getDueDate());
        response.setReturnDate(borrow.getReturnDate());
        response.setNotes(borrow.getNotes());
        
        return response;
    }
}
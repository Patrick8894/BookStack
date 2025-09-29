package com.bookstack.bookstack.book.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
public class BookInput {
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Author is required")
    private String author;
    
    @NotBlank(message = "ISBN is required")
    private String isbn;
    
    private String category;
    private String language;
    
    @NotNull(message = "Total copies is required")
    @Min(value = 1, message = "Total copies must be at least 1")
    private Integer totalCopies;
    
    @NotNull(message = "Available copies is required")
    @Min(value = 0, message = "Available copies must be at least 0")
    private Integer availableCopies;
    
}
package com.bookstack.bookstack.book.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@SQLDelete(sql = "UPDATE books SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL") // Automatically excludes soft-deleted books
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(unique = true, nullable = false, length = 20)
    private String isbn;

    @Column(length = 50)
    private String category;

    @Column(length = 30)
    private String language;

    @Column(name = "total_copies", nullable = false)
    private Integer totalCopies;

    @Column(name = "available_copies", nullable = false)
    private Integer availableCopies;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isAvailableForBorrowing() {
        return !isDeleted() && availableCopies != null && availableCopies > 0;
    }

    public boolean isFullyBorrowed() {
        return availableCopies != null && availableCopies == 0;
    }

    public Integer getBorrowedCopies() {
        if (totalCopies == null || availableCopies == null) {
            return 0;
        }
        return totalCopies - availableCopies;
    }
}
package com.bookstack.bookstack.book.repository;

import com.bookstack.bookstack.book.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    // Standard queries (automatically exclude soft-deleted books)
    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByAuthorContainingIgnoreCase(String author);
    List<Book> findByCategoryIgnoreCase(String category);
    List<Book> findByLanguageIgnoreCase(String language);
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);

    // Fixed query - cast parameters to text explicitly
    @Query("SELECT b FROM Book b WHERE " +
           "(:title IS NULL OR UPPER(b.title) LIKE UPPER(CONCAT('%', CAST(:title AS string), '%'))) AND " +
           "(:author IS NULL OR UPPER(b.author) LIKE UPPER(CONCAT('%', CAST(:author AS string), '%'))) AND " +
           "(:category IS NULL OR UPPER(b.category) = UPPER(CAST(:category AS string))) AND " +
           "(:language IS NULL OR UPPER(b.language) = UPPER(CAST(:language AS string))) AND " +
           "b.deletedAt IS NULL")
    List<Book> findActiveBooksByFilters(@Param("title") String title, 
                                       @Param("author") String author, 
                                       @Param("category") String category, 
                                       @Param("language") String language);

    // Alternative approach using native query for PostgreSQL
    @Query(value = "SELECT * FROM books WHERE " +
           "(:title IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:author IS NULL OR LOWER(author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
           "(:category IS NULL OR LOWER(category) = LOWER(:category)) AND " +
           "(:language IS NULL OR LOWER(language) = LOWER(:language)) AND " +
           "deleted_at IS NULL", nativeQuery = true)
    List<Book> findActiveBooksByFiltersNative(@Param("title") String title, 
                                             @Param("author") String author, 
                                             @Param("category") String category, 
                                             @Param("language") String language);

    // Soft delete specific queries
    @Query("SELECT b FROM Book b WHERE b.deletedAt IS NULL")
    List<Book> findAllActive();
    
    @Query("SELECT b FROM Book b WHERE b.deletedAt IS NOT NULL")
    List<Book> findAllDeleted();
    
    @Query("SELECT b FROM Book b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Book> findActiveById(@Param("id") Long id);
    
    @Query("SELECT b FROM Book b WHERE b.isbn = :isbn AND b.deletedAt IS NULL")
    Optional<Book> findActiveByIsbn(@Param("isbn") String isbn);
    
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Book b WHERE b.isbn = :isbn AND b.deletedAt IS NULL")
    boolean existsActiveByIsbn(@Param("isbn") String isbn);
    
    @Query("SELECT b FROM Book b WHERE UPPER(b.title) LIKE UPPER(CONCAT('%', :title, '%')) AND b.deletedAt IS NULL")
    List<Book> findActiveByTitleContainingIgnoreCase(@Param("title") String title);
    
    @Query("SELECT b FROM Book b WHERE UPPER(b.author) LIKE UPPER(CONCAT('%', :author, '%')) AND b.deletedAt IS NULL")
    List<Book> findActiveByAuthorContainingIgnoreCase(@Param("author") String author);
    
    @Query("SELECT b FROM Book b WHERE UPPER(b.category) = UPPER(:category) AND b.deletedAt IS NULL")
    List<Book> findActiveByCategoryIgnoreCase(@Param("category") String category);
    
    @Query("SELECT b FROM Book b WHERE UPPER(b.language) = UPPER(:language) AND b.deletedAt IS NULL")
    List<Book> findActiveByLanguageIgnoreCase(@Param("language") String language);
    
    @Query("SELECT b FROM Book b WHERE b.availableCopies > 0 AND b.deletedAt IS NULL")
    List<Book> findAvailableBooks();
    
    // Hard delete (physical deletion) - for admin use only
    @Modifying
    @Query("DELETE FROM Book b WHERE b.id = :id")
    void hardDeleteById(@Param("id") Long id);
    
    // Restore deleted book
    @Modifying
    @Query("UPDATE Book b SET b.deletedAt = NULL WHERE b.id = :id")
    void restoreById(@Param("id") Long id);
    
    // Check if book exists (including deleted)
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Book b WHERE b.id = :id")
    boolean existsByIdIncludingDeleted(@Param("id") Long id);
    
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Book b WHERE b.isbn = :isbn")
    boolean existsByIsbnIncludingDeleted(@Param("isbn") String isbn);
}
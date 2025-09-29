package com.bookstack.bookstack.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookstack.bookstack.user.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByRole(String role);
    List<User> findByUsernameContainingIgnoreCase(String username);
    
    // Soft delete specific queries
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllActive();
    
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL")
    List<User> findAllDeleted();
    
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveById(@Param("id") Long id);
    
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findActiveByUsername(@Param("username") String username);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    boolean existsActiveByUsername(@Param("username") String username);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.deletedAt IS NULL")
    List<User> findActiveByRole(@Param("role") String role);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')) AND u.deletedAt IS NULL")
    List<User> findActiveByUsernameContainingIgnoreCase(@Param("username") String username);
    
    // Hard delete (physical deletion) - for admin use only
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :id")
    void hardDeleteById(@Param("id") Long id);
    
    // Restore deleted user
    @Modifying
    @Query("UPDATE User u SET u.deletedAt = NULL WHERE u.id = :id")
    void restoreById(@Param("id") Long id);
    
    // Check if user exists (including deleted)
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.id = :id")
    boolean existsByIdIncludingDeleted(@Param("id") Long id);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username")
    boolean existsByUsernameIncludingDeleted(@Param("username") String username);
}
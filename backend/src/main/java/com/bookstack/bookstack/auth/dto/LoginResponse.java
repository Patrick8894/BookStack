package com.bookstack.bookstack.auth.dto;

import com.bookstack.bookstack.user.model.User;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserDto user;

    public LoginResponse(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }

    @Data
    // Nested UserDto to avoid exposing sensitive data like password
    public static class UserDto {
        private Long id;
        private String username;
        private String role;

        public UserDto(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.role = user.getRole();
        }
    }
}
package com.bookstack.bookstack.auth.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String role; // default = MEMBER
}
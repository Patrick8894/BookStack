package com.bookstack.bookstack.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookstack.bookstack.auth.annotation.RequireRole;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from BookStack (REST)!";
    }

    @GetMapping("/hello-secured")
    @RequireRole({"LIBRARIAN", "ADMIN"})
    public String helloSecured() {
        return "Hello from secured endpoint! Only LIBRARIAN or ADMIN can access this.";
    }
}
package com.bookstack.bookstack.test;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from BookStack (REST)!";
    }

    @GetMapping("/hello-secured")
    @PreAuthorize("hasRole('LIBRARIAN') or hasRole('ADMIN')")
    public String helloSecured() {
        return "Hello from secured endpoint! Only LIBRARIAN or ADMIN can access this.";
    }
}
package com.bookstack.bookstack.user.controller;

import com.bookstack.bookstack.user.dto.UserInput;
import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.service.UserService;
import com.bookstack.bookstack.auth.annotation.RequireRole;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import jakarta.validation.Valid;

@Controller
public class UserGraphQLController {
    private final UserService userService;

    public UserGraphQLController(UserService userService) {
        this.userService = userService;
    }

    @QueryMapping
    @RequireRole({"ADMIN"})
    public List<User> allUsers() {
        return userService.getAllUsers();
    }

    @QueryMapping
    @RequireRole({"ADMIN"})
    public User userById(@Argument Long id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    @RequireRole({"ADMIN"})
    public User userByUsername(@Argument String username) {
        return userService.getUserByUsername(username).orElse(null);
    }

    @QueryMapping
    @RequireRole({"ADMIN"})
    public List<User> searchUsersByUsername(@Argument String username) {
        return userService.searchUsersByUsername(username);
    }

    @QueryMapping
    @RequireRole({"ADMIN"})
    public List<User> usersByRole(@Argument String role) {
        return userService.getUsersByRole(role);
    }

    @MutationMapping
    @RequireRole({"ADMIN"})
    public User addUser(@Argument @Valid UserInput input) {
        return userService.createUser(input.getUsername(), input.getPassword(), input.getRole());
    }

    @MutationMapping
    @RequireRole({"ADMIN"})
    public User updateUser(@Argument Long id, @Argument @Valid UserInput input) {
        return userService.updateUser(id, input.getUsername(), input.getPassword(), input.getRole());
    }

    @MutationMapping
    @RequireRole({"ADMIN"})
    public User updateUserRole(@Argument Long id, @Argument String role) {
        return userService.updateUserRole(id, role);
    }

    @MutationMapping
    @RequireRole({"ADMIN"})
    public User updateUserPassword(@Argument Long id, @Argument String password) {
        return userService.updateUserPassword(id, password);
    }

    @MutationMapping
    @RequireRole({"ADMIN"})
    public Boolean deleteUser(@Argument Long id) {
        userService.deleteUser(id);
        return true;
    }
}
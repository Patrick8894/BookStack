package com.bookstack.bookstack.user.controller;

import com.bookstack.bookstack.user.dto.UserInput;
import com.bookstack.bookstack.user.model.User;
import com.bookstack.bookstack.user.service.UserService;
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

    // Queries
    @QueryMapping
    public List<User> allUsers() {
        return userService.getAllUsers();
    }

    @QueryMapping
    public User userById(@Argument Long id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    public User userByUsername(@Argument String username) {
        return userService.getUserByUsername(username).orElse(null);
    }

    @QueryMapping
    public List<User> usersByRole(@Argument String role) {
        return userService.getUsersByRole(role);
    }

    // Mutations
    @MutationMapping
    public User addUser(@Argument @Valid UserInput input) {
        return userService.createUser(input.getUsername(), input.getPassword(), input.getRole());
    }

    @MutationMapping
    public User updateUser(@Argument Long id, @Argument @Valid UserInput input) {
        return userService.updateUser(id, input.getUsername(), input.getPassword(), input.getRole());
    }

    @MutationMapping
    public User updateUserRole(@Argument Long id, @Argument String role) {
        return userService.updateUserRole(id, role);
    }

    @MutationMapping
    public User updateUserPassword(@Argument Long id, @Argument String password) {
        return userService.updateUserPassword(id, password);
    }

    @MutationMapping
    public Boolean deleteUser(@Argument Long id) {
        userService.deleteUser(id);
        return true;
    }
}
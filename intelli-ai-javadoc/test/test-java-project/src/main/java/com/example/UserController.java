package com.example;

import java.util.List;

public class UserController {
    private UserService userService;

    public UserController() {
        this.userService = new UserService();
    }

    public User getUser(int id) {
        return userService.findUserById(id);
    }

    public User createUser(String name, String email) {
        return userService.createUser(name, email);
    }

    public boolean updateUser(User user) {
        return userService.updateUser(user);
    }

    public List<User> getAllUsers() {
        return userService.findAll();
    }

    public User createUser(String name, String email) {
        return userService.createUser(name, email);
    }

    public boolean deleteUser(int id) {
        return userService.deleteUser(id);
    }

    public List<User> getAllUsers() {
        return userService.findAll();
    }
}


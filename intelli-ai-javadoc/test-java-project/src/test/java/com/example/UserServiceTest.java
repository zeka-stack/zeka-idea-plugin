package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class UserServiceTest {
    
    @Test
    public void testFindUserById_whenUserExists_shouldReturnUser() {
        UserService service = new UserService();
        User user = service.createUser("John Doe", "john@example.com");
        User found = service.findUserById(user.getId());
        Assertions.assertNotNull(found);
        Assertions.assertEquals("John Doe", found.getName());
    }
    
    @Test
    public void testCreateUser_shouldReturnNewUser() {
        UserService service = new UserService();
        User user = service.createUser("Jane Doe", "jane@example.com");
        Assertions.assertNotNull(user);
        Assertions.assertEquals("Jane Doe", user.getName());
        Assertions.assertEquals("jane@example.com", user.getEmail());
    }
    
    @Test
    public void testUpdateUser_whenUserExists_shouldReturnTrue() {
        UserService service = new UserService();
        User user = service.createUser("Test User", "test@example.com");
        user.setName("Updated User");
        boolean result = service.updateUser(user);
        Assertions.assertTrue(result);
    }
    
    @Test
    public void testDeleteUser_whenUserExists_shouldReturnTrue() {
        UserService service = new UserService();
        User user = service.createUser("Delete User", "delete@example.com");
        boolean result = service.deleteUser(user.getId());
        Assertions.assertTrue(result);
    }
}


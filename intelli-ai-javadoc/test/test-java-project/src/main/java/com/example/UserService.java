package com.example;

public class UserService {
    private UserRepository userRepository = new UserRepository();
    
    private User currentUser;
    
    public User findUserById(int id) {
        return userRepository.findById(id);
    }
    
    public User createUser(String name, String email) {
        User user = new User(name, email);
        userRepository.save(user);
        return user;
    }
    
    public boolean updateUser(User user) {
        return userRepository.update(user);
    }
    
    public boolean deleteUser(int id) {
        return userRepository.delete(id);
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}


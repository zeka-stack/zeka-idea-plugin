package com.example;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private List<User> users = new ArrayList<>();
    
    public User findById(int id) {
        for (User user : users) {
            if (user.getId() == id) {
                return user;
            }
        }
        return null;
    }
    
    public void save(User user) {
        users.add(user);
    }
    
    public boolean update(User user) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == user.getId()) {
                users.set(i, user);
                return true;
            }
        }
        return false;
    }
    
    public boolean delete(int id) {
        return users.removeIf(user -> user.getId() == id);
    }
    
    public List<User> findAll() {
        return new ArrayList<>(users);
    }
    
    public int count() {
        return users.size();
    }
    
    public void clear() {
        users.clear();
    }
}


package com.example;

/**
 * 用户服务类
 * <p> 提供用户相关的业务逻辑处理, 包括用户的查询, 创建, 更新, 删除以及当前用户的获取与设置等操作
 *
 * @author 1111
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.17
 * @since 1.0.0
 */
public class UserService {
    /** 用户仓库, 用于操作用户数据, 包括查询, 保存, 更新和删除用户信息 */
    private UserRepository userRepository = new UserRepository();

    private User currentUser;

    public User findUserById(int id) {
        return userRepository.findById(id);
    }

    /**
     * 创建并保存一个新用户
     * <p> 根据提供的姓名和邮箱创建用户对象, 并通过用户仓库保存到数据库中
     *
     * @param name  用户的姓名
     * @param email 用户的邮箱
     * @return 创建并保存后的用户对象
     */
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


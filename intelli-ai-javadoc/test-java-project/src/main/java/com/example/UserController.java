package com.example;

import java.util.List;

public class UserController {
    private UserService userService;

    public UserController() {
        this.userService = new UserService();
    }

    /**
     * 根据用户 ID 获取用户对象
     * <p>
     * 通过用户 ID 查找用户并返回用户对象
     *
     * @param id 用户 ID
     * @return 用户对象, 如果用户不存在则返回 null
     */
    public User getUser(int id) {
        return userService.findUserById(id);
    }

    /**package com.example;

     import java.util.List;

     public class UserController {
     private UserService userService;

     public UserController() {
     this.userService = new UserService();
     }

     /**
     * 根据用户 ID 获取用户对象
     * <p>
     * 通过用户 ID 查找用户并返回用户对象
     *
     * @param id 用户 ID
     * @return 用户对象, 如果用户不存在则返回 null
     */
    public User getUser(int id) {
        return userService.findUserById(id);
    }

    /**
     * 创建新用户
     * <p>
     * 根据提供的姓名和邮箱创建新用户, 并通过用户服务进行保存
     *
     * @param name  用户姓名
     * @param email 用户邮箱
     * @return 创建的用户对象
     */
    public User createUser(String name, String email) {
        return userService.createUser(name, email);
    }

    /**
     * 更新指定用户的信息
     * <p>
     * 调用用户服务更新用户数据
     *
     * @param user 要更新的用户对象
     * @return 更新操作是否成功
     */
    public boolean updateUser(User user) {
        return userService.updateUser(user);
    }

    /**
     * 删除指定 ID 的用户
     * <p>
     * 调用用户服务删除指定 ID 的用户
     *
     * @param id 用户 ID
     * @return 删除操作是否成功
     */
    public boolean deleteUser(int id) {
        return userService.deleteUser(id);
    }

    /**
     * 获取所有用户列表
     * <p>
     * 从用户服务中获取所有用户信息并返回列表
     *
     * @return 用户列表
     */
    public List<User> getAllUsers() {
        return userService.findAll();
    }
}


     * 创建新用户
     * <p>
     * 根据提供的姓名和邮箱创建新用户, 并通过用户服务进行保存
     *
     * @param name  用户姓名
     * @param email 用户邮箱
     * @return 创建的用户对象
     */
    public User createUser(String name, String email) {
        return userService.createUser(name, email);
    }

    /**
     * 更新指定用户的信息
     * <p>
     * 调用用户服务更新用户数据
     *
     * @param user 要更新的用户对象
     * @return 更新操作是否成功
     */
    public boolean updateUser(User user) {
        return userService.updateUser(user);
    }

    /**
     * 删除指定 ID 的用户
     * <p>
     * 调用用户服务删除指定 ID 的用户
     *
     * @param id 用户 ID
     * @return 删除操作是否成功
     */
    public boolean deleteUser(int id) {
        return userService.deleteUser(id);
    }

    /**
     * 获取所有用户列表
     * <p>
     * 从用户服务中获取所有用户信息并返回列表
     *
     * @return 用户列表
     */
    public List<User> getAllUsers() {
        return userService.findAll();
    }
}


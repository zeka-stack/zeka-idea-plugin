package com.example;

import java.util.List;

/**
 * 用户控制器类
 * <p>
 * 用于处理与用户相关的 HTTP 请求, 包括获取用户信息, 创建用户, 更新用户, 删除用户以及获取所有用户列表等操作. 该类作为业务逻辑的封装层, 将用户相关的请求映射到 UserService 中进行处理.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */
public class UserController {
    /** 用户服务接口, 用于处理与用户相关的业务逻辑 */
    private UserService userService;

    /**
     * 用户控制器的构造函数
     * <p>
     * 初始化用户控制器, 创建并设置用户服务实例
     *
     * @since 1.0
     */
    public UserController() {
        this.userService = new UserService();
    }

    /**
     * 根据用户 ID 获取用户对象
     * <p>
     * 通过用户 ID 查找对应的用户对象
     *
     * @param id 用户 ID
     * @return 用户对象, 若用户不存在则返回 null
     */
    public User getUser(int id) {
        return userService.findUserById(id);
    }

    /**
     * 根据用户 ID 获取用户对象
     * <p>
     * 通过用户 ID 查找用户并返回对应的 {@link User} 实例
     *
     * @param id 用户 ID
     * @return 用户对象
     */
    public User getUser(int id) {
        return userService.findUserById(id);
    }

    /**
     * 创建新用户
     * <p>
     * 根据提供的姓名和邮箱信息创建新用户, 并通过用户服务返回创建的用户对象
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
     * 调用用户服务层更新用户数据
     *
     * @param user 要更新的用户对象
     * @return 更新操作是否成功
     */
    public boolean updateUser(User user) {
        return userService.updateUser(user);
    }

    /**
     * 根据用户 ID 删除用户
     * <p>
     * 通过用户 ID 调用用户服务删除对应用户
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
/** 用户姓名 */
/** 用户姓名 */
/** 用户姓名 */
         * @param name  用户姓名
/** 用户邮箱 */
/** 用户邮箱 */
/** 用户邮箱 */
     * @param email 用户邮箱
     * @return 创建的用户对象
     */

/**
 * 创建并返回一个新的用户
 * <p>
 * 根据提供的姓名和邮箱地址调用 {@code userService} 创建用户, 并返回创建后的 {@link User} 对象.
 *
 * @param name  用户姓名
 * @param email 用户邮箱地址
 * @return 创建成功的 {@link User} 对象
 */
public User createUser(String name, String email) {
        return userService.createUser(name, email);
    }

/**
 * 更新指定用户的信息
 * <p>
 * 调用用户服务层更新用户数据
 *
 * @param user 要更新的用户对象
 * @return 更新操作是否成功
 */
public boolean updateUser(User user) {
        return userService.updateUser(user);
    }

/**
 * 根据用户 ID 删除用户
 * <p>
 * 通过用户 ID 查找用户并执行删除操作
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
 * 通过用户服务获取所有用户信息并返回
 *
 * @return 用户列表
 */
public List<User> getAllUsers() {
        return userService.findAll();
    }
}


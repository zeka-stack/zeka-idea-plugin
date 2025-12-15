package com.example;

import java.util.List;

/**
 * 用户控制器类
 * <p>
 * 用于处理与用户相关的 HTTP 请求, 包括获取用户信息, 创建用户, 更新用户, 删除用户以及获取所有用户等操作. 该类主要负责接收外部请求并调用对应的业务逻辑处理类 (UserService) 进行数据操作.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */
public class UserController {
    /** 用户服务, 用于处理用户相关业务逻辑 */
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
     * 根据用户 ID 获取用户信息
     * <p>
     * 通过用户 ID 查找并返回对应的用户对象
     *
     * @param id 用户 ID
     * @return 用户对象
     */
    public User getUser(int id) {
        return userService.findUserById(id);
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
     * 调用用户服务层更新用户数据, 返回操作是否成功
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
     * 获取所有用户
     * <p>
     * 调用 {@code userService.findAll()} 返回所有用户列表
     *
     * @return 所有用户的列表
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
/** 用户姓名 */
/** 用户姓名 */
         * @param name  用户姓名
/** 用户邮箱 */
/** 用户邮箱 */
/** 用户邮箱 */
/** 用户邮箱 */
/** 用户邮箱 */
     * @param email 用户邮箱
     * @return 创建的用户对象
     */

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
 * 调用用户服务层更新用户数据, 返回操作是否成功
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
 * 调用 {@code userService.findAll()} 返回系统中所有 {@link User} 对象的列表.
 *
 * @return 所有用户的 {@link List}, 若无用户则返回空列表
 */
public List<User> getAllUsers() {
        return userService.findAll();
    }
}


package com.example;

/**
 * 用户服务类
 * <p>
 * 提供用户相关的业务逻辑处理, 包括用户的查询, 创建, 更新, 删除以及当前用户的获取与设置等操作
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */
public class UserService {
    /** 用户数据访问对象, 用于操作用户相关数据 */
    private UserRepository userRepository = new UserRepository();

    /** 当前用户 */
    private User currentUser;

    /**
     * 根据用户 ID 查找用户
     * <p>
     * 通过指定的用户 ID 从用户仓库中查找对应的用户对象
     *
     * @param id 用户 ID
     * @return 匹配 ID 的用户对象, 如果未找到则返回 null
     */
    public User findUserById(int id) {
        return userRepository.findById(id);
    }

    /**
     * 创建并保存一个新用户
     * <p>
     * 根据提供的姓名和邮箱创建用户对象, 并将其保存到用户仓库中, 最后返回创建的用户对象
     *
     * @param name  用户姓名
     * @param email 用户邮箱
     * @return 创建并保存后的用户对象
     */
    public User createUser(String name, String email) {
        User user = new User(name, email);
        userRepository.save(user);
        return user;
    }

    /**
     * 更新指定用户的信息
     * <p>
     * 通过用户对象更新数据库中的用户信息
     *
     * @param user 要更新的用户对象
     * @return 更新操作是否成功
     */
    public boolean updateUser(User user) {
        return userRepository.update(user);
    }

    /**
     * 根据指定 ID 删除用户
     * <p>
     * 通过用户 ID 从数据库中删除对应用户记录
     *
     * @param id 用户 ID
     * @return 删除操作是否成功
     */
    public boolean deleteUser(int id) {
        return userRepository.delete(id);
    }

    /**
     * 获取当前用户信息
     * <p>
     * 返回当前登录用户的对象, 通常用于权限校验或用户信息获取
     *
     * @return 当前用户对象
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * 设置当前用户
     * <p>
     * 将传入的 {@code currentUser} 赋值给当前实例的 {@code currentUser} 字段, 以便后续业务逻辑使用.
     *
     * @param currentUser 当前用户对象, 可能为 {@code null}, 表示无登录用户
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}


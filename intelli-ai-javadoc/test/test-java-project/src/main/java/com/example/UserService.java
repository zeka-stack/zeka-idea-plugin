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

    /** 当前用户对象 */
    private User currentUser;

    /**
     * 根据用户 ID 查找用户
     * <p>
     * 通过用户 ID 在用户仓库中查找并返回对应的用户对象
     *
     * @param id 用户 ID
     * @return 用户对象, 如果未找到则返回 {@code null}
     */
    public User findUserById(int id) {
        return userRepository.findById(id);
    }

    /**
     * 创建并保存一个用户
     * <p>
     * 根据提供的姓名和邮箱创建用户对象, 并保存到数据库中
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
     * 更新指定用户信息
     * <p>
     * 根据传入的用户对象更新对应用户信息, 返回更新是否成功
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
     * 获取当前用户
     * <p>
     * 返回当前已登录或已设置的用户对象
     *
     * @return 当前用户对象
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * 设置当前用户
     * <p>
     * 将指定的用户对象设置为当前用户
     *
     * @param currentUser 当前用户对象
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}


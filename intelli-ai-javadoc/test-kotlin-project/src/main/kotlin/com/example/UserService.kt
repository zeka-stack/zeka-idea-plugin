package com.example

/**
 * 用户服务类
 * <p>
 * 负责管理用户相关业务逻辑, 提供用户的查询, 创建, 更新和删除等基本操作, 并通过 {@link UserRepository} 与数据层进行交互.
 * 同时维护 {@code currentUser} 字段, 方便在业务流程中快速获取当前操作的用户实例.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.09
 * @since 1.0.0
 */
class UserService {
    /** 用户数据仓库 */
    private val userRepository: UserRepository = UserRepository()

    /** 当前用户 */
    var currentUser: User? = null

    /**
     * 根据用户 ID 查找用户
     * <p>
     * 通过传入的用户 ID 在用户仓库中查询对应的用户信息.
     *
     * @param id 用户的唯一标识
     * @return 对应的 {@link User} 对象; 若未找到则返回 {@code null}
     */
    fun findUserById(id: Int): User? {
        return userRepository.findById(id)
    }

    /**
     * 创建并保存用户
     * <p>
     * 根据传入的用户名和邮箱创建 {@link User} 实例, 调用 {@code userRepository.save} 将其持久化,
     * 并返回已保存的用户对象.
     *
     * @param name  用户名
     * @param email 电子邮件地址
     * @return 已创建并保存的 {@link User} 对象
     */
    fun createUser(name: String, email: String): User {
        /** 用户对象, 包含姓名和邮箱信息 */
        val user = User(name, email)
        userRepository.save(user)
        return user
    }

    /**
     * 更新用户信息
     * <p>
     * 将传入的 {@code user} 对象持久化到 {@code userRepository}, 并返回更新是否成功的布尔值.
     *
     * @param user 要更新的用户对象
     * @return {@code true} 表示更新成功,{@code false} 表示更新失败
     */
    fun updateUser(user: User): Boolean {
        return userRepository.update(user)
    }

    /**
     * 删除指定用户
     * <p>
     * 根据给定的用户 ID 调用 {@code userRepository.delete} 方法删除用户, 并返回删除是否成功的布尔值.
     *
     * @param id 要删除的用户 ID
     * @return 若删除成功返回 {@code true}, 否则返回 {@code false}
     */
    fun deleteUser(id: Int): Boolean {
        return userRepository.delete(id)
    }
}


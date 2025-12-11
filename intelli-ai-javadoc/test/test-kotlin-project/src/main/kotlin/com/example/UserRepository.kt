package com.example

/**
 * 用户仓库类
 * <p>
 * 提供用户数据的持久化操作, 包括用户的查询, 创建, 更新, 删除和获取所有用户等操作. 该类使用内存列表来存储用户数据, 适用于简单的数据存储场景.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.11
 * @since 1.0.0
 */
class UserRepository {
    /** 用户列表 */
    private val users = mutableListOf<User>()

    /**
     * 根据指定的 ID 查找用户
     * <p>
     * 在用户列表中查找具有指定 ID 的用户, 并返回该用户对象. 如果未找到匹配的用户, 则返回 null.
     *
     * @param id 要查找的用户 ID
     * @return 匹配 ID 的用户对象, 如果未找到则返回 null
     */
    fun findById(id: Int): User? {
        return users.find { it.id == id }
    }

    /**
     * 保存用户
     * <p>
     * 将指定的用户添加到内部用户集合中
     *
     * @param user 要保存的用户对象
     */
    fun save(user: User) {
        users.add(user)
    }

    /**
     * 更新用户信息
     * <p>
     * 在内部用户列表中查找与给定 {@code user} 对象同一 {@code id} 的用户,
     * 若找到则用新的 {@code user} 替换原有条目并返回 {@code true};
     * 若未找到对应用户则保持列表不变并返回 {@code false}.
     *
     * @param user 要更新的用户对象
     * @return 若成功更新用户则返回 {@code true}, 否则返回 {@code false}
     */
    fun update(user: User): Boolean {
        val index = users.indexOfFirst { it.id == user.id }
        return if (index >= 0) {
            users[index] = user
            true
        } else {
            false
        }
    }

    /**
     * 删除指定 ID 的用户
     * <p>
     * 在用户集合中查找与给定 ID 匹配的用户并将其移除
     *
     * @param id 要删除的用户 ID
     * @return 若成功删除至少一个用户则返回 {@code true}, 否则返回 {@code false}
     */
    fun delete(id: Int): Boolean {
        return users.removeIf { it.id == id }
    }

    /**
     * 获取所有用户
     * <p>
     * 返回当前用户集合的不可变副本, 避免外部修改内部列表
     *
     * @return 所有用户的列表
     */
    fun findAll(): List<User> {
        return users.toList()
    }
}


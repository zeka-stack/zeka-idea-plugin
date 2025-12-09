package com.example

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * UserServiceTest
 * <p>
 * 该测试类用于验证 UserService 的核心业务逻辑, 包括用户创建, 查询和更新等功能. 通过 JUnit 5 的 @Test 注解编写了若干单元测试, 确保服务层在不同场景下的正确性与健壮性.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.09
 * @since 1.0.0
 */
class UserServiceTest {
    /**
     * 测试根据用户 ID 查找用户功能
     * <p>
     * 测试场景: 已存在用户时调用 {@link UserService#findUserById(Long)} 方法
     * 预期结果: 返回的用户对象不为 null, 且名称与创建时一致
     */
    @Test
    fun testFindUserById_whenUserExists_shouldReturnUser() {
        /** UserService 实例, 用于处理用户相关业务逻辑 */
        val service = UserService()
        val user = service.createUser("John Doe", "john@example.com")
        val found = service.findUserById(user.id)
        assertNotNull(found)
        assertEquals("John Doe", found?.name)
    }

    /**
     * 测试创建用户功能
     * <p>
     * 测试场景: 调用 {@link UserService#createUser(String, String)} 方法创建新用户
     * 预期结果: 返回非空 User 对象, 且 {@code name} 与 {@code email} 与传入参数一致
     */
    @Test
    fun testCreateUser_shouldReturnNewUser() {
        /** UserService 实例, 用于处理用户相关业务逻辑 */
        val service = UserService()
        val user = service.createUser("Jane Doe", "jane@example.com")
        assertNotNull(user)
        assertEquals("Jane Doe", user.name)
        assertEquals("jane@example.com", user.email)
    }

    /**
     * 测试更新用户功能
     * <p>
     * 测试场景: 当用户已存在时
     * 预期结果: 更新成功, 返回 true
     */
    @Test
    fun testUpdateUser_whenUserExists_shouldReturnTrue() {
        /** UserService 实例, 用于处理用户相关业务逻辑 */
        val service = UserService()
        val user = service.createUser("Test User", "test@example.com")

        /** 用户对象的副本, 名称已更新 */
        val updated = user.copy(name = "Updated User")
        val result = service.updateUser(updated)
        assertTrue(result)
    }
}


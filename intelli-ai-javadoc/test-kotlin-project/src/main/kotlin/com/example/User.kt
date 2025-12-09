package com.example

/**
 * 用户数据类
 * <p>
 * 用于表示用户信息, 包含姓名, 邮箱, 编号等基本属性, 并提供
 * 计算显示名称和判断是否成年等辅助方法.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.09
 * @since 1.0.0
 */
data class User(
    val name: String,
    val email: String,
    val id: Int = 0
) {
    var age: Int = 0
    var address: String? = null

    /**
     * 获取显示名称
     * <p>
     * 以“姓名 (邮箱)”的格式返回用户的显示名称
     *
     * @return 以“姓名 (邮箱)”格式的字符串
     */
    fun getDisplayName(): String {
        return "$name ($email)"
    }

    /**
     * 判断是否为成年人
     * <p>
     * 根据实例中的 {@code age} 字段判断当前对象是否已达到成年年龄 (18 岁及以上).
     *
     * @return {@code true} 表示年龄大于等于 18 岁;{@code false} 表示年龄小于 18 岁
     */
    fun isAdult(): Boolean {
        return age >= 18
    }
}


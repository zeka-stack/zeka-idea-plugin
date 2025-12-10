package com.example


/**
 * 用户数据类
 * <p>
 * 封装用户基本信息, 包括姓名, 邮箱, 编号, 年龄, 地址等属性, 并提供获取显示名称和判断是否成年等功能
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.10
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
     * 以 "name(email)" 的格式返回用户的显示名称
     *
     * @return 用户的显示名称, 格式为 "name(email)"
     */
    fun getDisplayName(): String {
        return "$name ($email)"
    }

    /**
     * 判断是否为成年人
     * <p>
     * 根据当前对象的 {@code age} 字段判断是否已满 18 岁
     *
     * @return {@code true} 表示已成年, 否则 {@code false}
     */
    fun isAdult(): Boolean {
        return age >= 18
    }
}


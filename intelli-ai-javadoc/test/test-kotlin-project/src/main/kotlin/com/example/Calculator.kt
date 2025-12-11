package com.example

/**
 * Calculator 对象
 * <p>
 * 提供基本的算术运算功能, 包括加, 减, 乘, 除等操作. 除法方法在除数为 0 时会抛出 {@link IllegalArgumentException}.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.09
 * @since 1.0.0
 */
object Calculator {
    /**
     * 计算两个整数的和
     *
     * @param a 第一个加数
     * @param b 第二个加数
     * @return 两个整数相加的结果
     */
    fun add(a: Int, b: Int): Int {
        return a + b
    }

    /**
     * 计算两个整数相减的结果
     * <p>
     * 返回 a 减去 b 的值
     *
     * @param a 被减数
     * @param b 减数
     * @return a 与 b 的差值
     */
    fun subtract(a: Int, b: Int): Int {
        return a - b
    }

    /**
     * 计算两个整数的乘积
     *
     * @param a 第一个整数
     * @param b 第二个整数
     * @return 两个整数相乘的结果
     */
    fun multiply(a: Int, b: Int): Int {
        return a * b
    }

    /**
     * 计算两个整数相除的结果
     * <p>
     * 将整数 {@code a} 转换为 {@code Double} 并除以整数 {@code b}, 返回除法结果.
     * 若除数 {@code b} 为 0, 则抛出 {@link IllegalArgumentException}, 提示不允许除以零.
     *
     * @param a 被除数
     * @param b 除数
     * @return {@code a} 除以 {@code b} 的结果
     * @throws IllegalArgumentException 当 {@code b} 为 0 时抛出
     */
    fun divide(a: Int, b: Int): Double {
        if (b == 0) {
            throw IllegalArgumentException("Division by zero is not allowed")
        }
        return a.toDouble() / b
    }
}


package com.example;

/**
 * 计算器工具类
 * <p>
 * 提供基本的数学运算功能, 包括加法, 减法, 乘法, 除法和幂运算
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */
public class Calculator {

    /**
     * 对两个整数进行相加运算
     * <p>
     * 该方法接收两个整数参数, 返回它们的和
     *
     * @param a 第一个整数
     * @param b 第二个整数
     * @return 两个整数的和
     */
    public static int add(int a, int b) {
        return a + b;
    }

    /**
     * 两个整数相减
     * <p>
     * 返回两个整数相减的结果
     *
     * @param a 被减数
     * @param b 减数
     * @return a 减去 b 的结果
     */
    public static int subtract(int a, int b) {
        return a - b;
    }

    /**
     * 计算两个整数的乘积
     * <p>
     * 返回参数 {@code a} 与 {@code b} 的乘积
     *
     * @param a 第一个整数
     * @param b 第二个整数
     * @return 两个整数相乘的结果
     */
    public static int multiply(int a, int b) {
        return a * b;
    }

    /**
     * 执行两个整数的除法运算
     * <p>
     * 该方法接收两个整数参数, 执行除法运算并返回结果. 如果除数为零, 则抛出异常.
     *
     * @param a 被除数
     * @param b 除数
     * @return 除法运算结果
     * @throws IllegalArgumentException 当除数为零时抛出
     */
    public static double divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("Division by zero is not allowed");
        }
        return (double) a / b;
    }

    /**
     * 计算给定底数的指数次幂
     * <p>
     * 该方法通过循环方式计算底数的指数次幂, 适用于非负整数指数
     *
     * @param base     底数
     * @param exponent 指数
     * @return 底数的指数次幂结果
     */
    public static int power(int base, int exponent) {
        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= base;
        }
        return result;
    }
}


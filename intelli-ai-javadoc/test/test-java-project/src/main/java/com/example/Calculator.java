package com.example;

/**
 * 计算器工具类
 * <p> 提供基本的数学运算功能, 包括加法, 减法, 乘法, 除法和幂运算
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
     * 返回参数 a 减去参数 b 的结果
     *
     * @param a 被减数
     * @param b 减数
     * @return a - b 的结果
     */
    public static int subtract(int a, int b) {
        return a - b;
    }

    /**
     * 两个整数相乘并返回结果
     * <p>
     * 该方法接收两个整数参数, 返回它们的乘积
     *
     * @param a 第一个整数
     * @param b 第二个整数
     * @return 两个整数的乘积
     */
    public static int multiply(int a, int b) {
        return a * b;
    }

    /**
     * 计算两个整数的除法结果
     * <p>
     * 该方法返回整数 {@code a} 除以整数 {@code b} 的结果, 结果类型为 {@code double}. 若除数 {@code b} 为 0, 则抛出 {@link IllegalArgumentException}.
     *
     * @param a 被除数
     * @param b 除数
     * @return {@code a} 除以 {@code b} 的结果, 类型为 {@code double}
     * @throws IllegalArgumentException 当除数为 0 时抛出
     */
    public static double divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("Division by zero is not allowed");
        }
        return (double) a / b;
    }

    /**
     * 计算整数的幂
     * <p>
     * 通过循环将基数乘以自身指数次得到结果
     *
     * @param base     底数
     * @param exponent 指数, 必须为非负整数
     * @return base 的 exponent 次幂
     */
    public static int power(int base, int exponent) {
        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= base;
        }
        return result;
    }
}


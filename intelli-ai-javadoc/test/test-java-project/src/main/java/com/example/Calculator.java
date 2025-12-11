package com.example;

public class Calculator {

    public static int add(int a, int b) {
        return a + b;
    }

    public static int subtract(int a, int b) {
        return a - b;
    }

    public static int multiply(int a, int b) {
        return a * b;
    }

    public static double divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("Division by zero is not allowed");
        }
        return (double) a / b;
    }

    /**
     * 计算给定基数的指数次幂
     * <p>
     * 该方法通过循环方式计算基数的指数次幂, 适用于非负整数指数.
     *
     * @param base     基数
     * @param exponent 指数
     * @return 基数的指数次幂结果
     */
    public static int power(int base, int exponent) {
        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result *= base;
        }
        return result;
    }
}


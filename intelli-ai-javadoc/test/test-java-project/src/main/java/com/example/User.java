package com.example;

/**
 * 用户实体类
 * <p>
 * 该类封装了用户的基本信息, 包括姓名, 邮箱, 编号, 年龄和地址等属性, 并提供相应的构造方法, 访问器和业务方法.
 * <p>
 * 主要功能:
 * <ul>
 *   <li> 构造用户对象, 支持默认编号和自定义编号的两种构造方式.</li>
 *   <li> 获取和设置用户的姓名, 邮箱, 编号, 年龄和地址.</li>
 *   <li> 返回用户的显示名称 (姓名 + 邮箱).</li>
 *   <li> 判断用户是否已成年 (年龄≥18).</li>
 * </ul>
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */
public class User {
    /** 名称 */
    private String name;
    /** 用于存储用户的电子邮件地址 */
    private String email;
    /** 主键 ID */
    private int id;
    /** 年龄 */
    private int age;
    /** 用户地址 */
    private String address;

    /**
     * 构造一个新的 {@link User} 实例.
     * <p>
     * 使用指定的姓名和邮箱创建用户, 并将用户 ID 初始化为 0.
     *
     * @param name  用户姓名
     * @param email 用户邮箱
     */
    public User(String name, String email) {
        this.name = name;
        this.email = email;
        this.id = 0;
    }

    /**
     * 构造一个 {@code User} 实例.
     * <p>
     * 使用指定的姓名, 邮箱和编号初始化用户对象.
     *
     * @param name  用户姓名
     * @param email 用户邮箱
     * @param id    用户编号
     */
    public User(String name, String email, int id) {
        this.name = name;
        this.email = email;
        this.id = id;
    }

    /**
     * 获取显示名称
     * <p>
     * 通过将姓名和邮箱拼接成格式 {@code name(email)} 返回显示名称
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return name + " (" + email + ")";
    }

    /**
     * 判断当前对象是否为成年人
     * <p>
     * 根据年龄判断是否达到法定成年年龄 (18 岁及以上)
     *
     * @return 如果年龄大于等于 18, 返回 true; 否则返回 false
     */
    public boolean isAdult() {
        return age >= 18;
    }

    /**
     * 获取当前对象的名称
     * <p>
     * 返回该对象的名称属性值
     *
     * @return 当前对象的名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置当前对象的名称属性
     * <p>
     * 将传入的名称赋值给当前对象的 name 字段
     *
     * @param name 要设置的名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取用户的电子邮件地址
     * <p>
     * 返回当前用户的电子邮件地址
     *
     * @return 用户的电子邮件地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置用户的邮箱地址
     * <p>
     * 将传入的邮箱地址赋值给当前用户对象
     *
     * @param email 邮箱地址
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取当前对象的唯一标识符
     * <p>
     * 返回该对象内部存储的唯一 ID 值
     *
     * @return 当前对象的唯一标识符
     */
    public int getId() {
        return id;
    }

    /**
     * 设置实体的唯一标识符
     * <p>
     * 将传入的整数值赋给当前对象的 id 属性
     *
     * @param id 要设置的唯一标识符
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * 获取用户的年龄
     * <p>
     * 返回当前用户的年龄值
     *
     * @return 用户的年龄
     */
    public int getAge() {
        return age;
    }

    /**
     * 设置用户的年龄
     * <p>
     * 将传入的年龄值赋给当前用户对象的 age 属性
     *
     * @param age 用户的年龄
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * 获取地址信息
     * <p>
     * 返回当前对象存储的地址信息
     *
     * @return 地址信息
     */
    public String getAddress() {
        return address;
    }

    /**
     * 设置用户的地址信息
     * <p>
     * 将传入的地址字符串赋值给当前对象的地址属性
     *
     * @param address 地址信息
     */
    public void setAddress(String address) {
        this.address = address;
    }
}


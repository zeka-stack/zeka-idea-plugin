package com.example;

/**
 * 用户数据类
 * <p> 用于表示系统中的用户实体, 包含用户的基本信息如姓名, 邮箱,ID, 年龄和地址等属性, 并提供相关访问方法和业务逻辑判断
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
    /** 用户的电子邮件地址 */
    private String email;
    /** ID */
    private int id;
    /** 年龄 */
    private int age;
    /** 用户地址 */
    private String address;

    /**
     * 构造一个 User 对象
     * <p>
     * 初始化用户名称和邮箱, 用户 ID 默认设置为 0
     *
     * @param name  用户名称
     * @param email 用户邮箱
     */
    public User(String name, String email) {
        this.name = name;
        this.email = email;
        this.id = 0;
    }

    /**
     * 用户构造函数
     * <p>
     * 初始化一个 User 对象, 设置用户名称, 电子邮件和用户 ID
     *
     * @param name  用户名称
     * @param email 电子邮件地址
     * @param id    用户 ID
     */
    public User(String name, String email, int id) {
        this.name = name;
        this.email = email;
        this.id = id;
    }

    /**
     * 获取用户的显示名称
     * <p>
     * 返回用户名称和邮箱的组合字符串, 格式为 "名称 (邮箱)"
     *
     * @param name  用户名称
     * @param email 用户邮箱
     * @return 用户的显示名称, 格式为 "名称 (邮箱)"
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
     * 获取名称
     * <p>
     * 返回当前对象的名称字符串
     *
     * @return 名称字符串
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
     * 获取当前用户的电子邮件地址
     * <p>
     * 返回当前用户绑定的电子邮件地址
     *
     * @return 用户的电子邮件地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置用户的邮箱地址
     * <p>
     * 将传入的邮箱地址赋值给当前用户的邮箱字段
     *
     * @param email 邮箱地址
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取当前对象的唯一标识符
     * <p>
     * 返回该对象的内部唯一标识符值
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
     * 返回当前用户对象的年龄属性值
     *
     * @return 用户的年龄
     */
    public int getAge() {
        return age;
    }

    /**
     * 设置用户年龄
     * <p>
     * 将指定的年龄值赋给当前用户对象
     *
     * @param age 用户年龄
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * 获取地址信息
     * <p>
     * 返回当前对象存储的地址值
     *
     * @return 地址信息
     */
    public String getAddress() {
        return address;
    }

    /**
     * 设置用户地址
     * <p>
     * 将指定的地址赋值给当前对象的地址属性
     *
     * @param address 地址信息
     */
    public void setAddress(String address) {
        this.address = address;
    }
}


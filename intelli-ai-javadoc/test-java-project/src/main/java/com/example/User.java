package com.example;

public class User {
    /** Name */
    private String name;
    /** Email address */
    private String email;
    /** Identifier */
    private int id;
    /** Age of the person */
    private int age;
    /** Address */
    private String address;

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

    public String getDisplayName() {
        return name + " (" + email + ")";
    }

    public boolean isAdult() {
        return age >= 18;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

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

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}


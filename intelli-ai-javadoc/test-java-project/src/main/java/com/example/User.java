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


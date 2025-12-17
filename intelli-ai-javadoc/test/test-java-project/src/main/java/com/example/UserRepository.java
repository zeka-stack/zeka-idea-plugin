package com.example;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户仓库类
 * <p> 提供对用户数据的增删改查操作, 用于管理用户集合. 支持根据 ID 查找用户, 保存用户, 更新用户, 删除用户, 获取所有用户, 统计用户数量以及清空用户集合等功能.
 *
 * @author 1111
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.17
 * @since 1.0.0
 */
public class UserRepository {
    /** 用户数据集合 */
    private List<User> users = new ArrayList<>();

    /**
     * 根据用户 ID 查找用户
     * <p> 遍历用户列表, 根据指定的用户 ID 查找对应的用户对象, 若找到则返回该用户, 否则返回 null
     *
     * @param id 用户 ID
     * @return 匹配 ID 的用户对象, 若未找到则返回 null
     */
    public User findById(int id) {
        for (User user : users) {
            if (user.getId() == id) {
                return user;
            }
        }
        return null;
    }

    /**
     * 将用户保存到用户列表中
     * <p> 将指定的用户对象添加到用户列表中
     *
     * @param user 要保存的用户对象
     */
    public void save(User user) {
        users.add(user);
    }

    /**
     * 更新指定 ID 的用户信息
     * <p> 根据用户 ID 查找并更新对应的用户对象, 若用户存在则返回 true, 否则返回 false
     *
     * @param user 要更新的用户对象, 必须包含有效的 ID
     * @return 如果用户存在并成功更新则返回 true, 否则返回 false
     */
    public boolean update(User user) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == user.getId()) {
                users.set(i, user);
                return true;
            }
        }
        return false;
    }

    /**
     * 根据用户 ID 删除用户
     * <p> 通过用户 ID 查找并删除对应的用户, 返回是否删除成功
     *
     * @param id 用户 ID
     * @return 如果用户存在且删除成功, 返回 true; 否则返回 false
     */
    public boolean delete(int id) {
        return users.removeIf(user -> user.getId() == id);
    }

    /**
     * 获取所有用户列表
     * <p> 返回当前存储的所有用户数据的副本, 确保原始数据不被外部直接修改
     *
     * @return 用户列表的不可变副本
     */
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    /**
     * 获取用户列表中的总用户数
     * <p> 返回用户集合中元素的数量
     *
     * @return 用户总数
     */
    public int count() {
        return users.size();
    }

    /**
     * 清除所有用户数据
     * <p> 将用户列表清空, 移除所有存储的用户信息
     *
     * @since 1.0
     */
    public void clear() {
        users.clear();
    }
}


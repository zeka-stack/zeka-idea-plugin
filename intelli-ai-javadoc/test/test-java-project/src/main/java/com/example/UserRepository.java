package com.example;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户仓库类
 * <p> 提供对用户数据的增删改查操作, 用于管理用户集合. 支持根据 ID 查找用户, 保存用户, 更新用户, 删除用户, 获取所有用户, 统计用户数量以及清空用户列表等功能.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */
public class UserRepository {
    /** 用户列表 */
    private List<User> users = new ArrayList<>();

    /**
     * 根据用户 ID 查找用户
     * <p>
     * 遍历用户列表, 根据指定的用户 ID 查找对应的用户对象, 若找到则返回, 否则返回 null
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
     * <p>
     * 将传入的用户对象添加到内部维护的用户列表中
     *
     * @param user 要保存的用户对象
     */
    public void save(User user) {
        users.add(user);
    }

    /**
     * 更新指定用户信息
     * <p>
     * 根据传入的用户对象, 查找并更新用户列表中对应 ID 的用户信息. 若找到匹配的用户, 则更新并返回 true; 否则返回 false.
     *
     * @param user 要更新的用户对象
     * @return 如果用户信息更新成功则返回 true, 否则返回 false
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
     * 根据指定 ID 删除用户
     * <p>
     * 通过用户 ID 从用户列表中移除对应用户, 若用户存在则返回 true, 否则返回 false
     *
     * @param id 用户 ID
     * @return 若用户被成功删除则返回 true, 否则返回 false
     */
    public boolean delete(int id) {
        return users.removeIf(user -> user.getId() == id);
    }

    /**
     * 获取所有用户列表
     * <p>
     * 返回系统中存储的所有用户数据
     *
     * @return 用户列表
     */
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    /**
     * 返回用户列表的大小
     * <p>
     * 获取当前用户集合中的元素数量
     *
     * @return 用户列表的大小
     */
    public int count() {
        return users.size();
    }

    /**
     * 清除所有用户数据
     * <p>
     * 从用户集合中移除所有元素, 使集合变为空
     *
     * @since 1.0
     */
    public void clear() {
        users.clear();
    }
}


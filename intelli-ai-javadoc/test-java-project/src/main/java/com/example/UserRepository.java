package com.example;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户仓库类
 * <p>
 * 提供对用户数据的持久化操作, 包括根据 ID 查找用户, 保存用户, 更新用户, 删除用户, 获取所有用户, 统计用户数量以及清空用户列表等功能
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.11
 * @since 1.0.0
 */
public class UserRepository {
    private List<User> users = new ArrayList<>();

    public User findById(int id) {
        for (User user : users) {
            if (user.getId() == id) {
                return user;
            }
        }
        return null;
    }

    public void save(User user) {
        users.add(user);
    }

    /**
     * 更新指定用户的信息
     * <p>
     * 根据用户 ID 在用户列表中查找匹配的用户, 若找到则更新该用户信息并返回 true, 否则返回 false
     *
     * @param user 要更新的用户对象
     * @return 如果用户存在且更新成功则返回 true, 否则返回 false
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

    public boolean delete(int id) {
        return users.removeIf(user -> user.getId() == id);
    }

    /**
     * 返回所有用户列表
     * <p>
     * 获取并返回系统中所有用户的列表
     *
     * @return 用户列表
     */
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    public int count() {
        return users.size();
    }

    public void clear() {
        users.clear();
    }
}


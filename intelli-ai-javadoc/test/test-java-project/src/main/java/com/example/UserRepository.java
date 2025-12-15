package com.example;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户仓储类
 * <p>
 * 负责对 User 对象进行增删改查等基本数据操作, 使用内存列表模拟持久化存储.
 * 提供按 ID 查询, 保存, 更新, 删除, 获取全部列表, 计数以及清空等方法.
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
     * 保存用户
     * <p>
     * 将指定的用户添加到用户集合中
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
     * @return 如果用户更新成功返回 true, 否则返回 false
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
     * 通过 ID 查找并删除对应的用户对象, 若用户存在则返回 true, 否则返回 false
     *
     * @param id 用户 ID
     * @return 删除是否成功,true 表示用户存在并被删除,false 表示用户不存在或删除失败
     */
    public boolean delete(int id) {
        return users.removeIf(user -> user.getId() == id);
    }

    /**
     * 获取所有用户列表
     * <p>
     * 返回系统中所有用户的列表
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
     * 清空用户集合
     * <p>
     * 该方法会移除 {@code users} 集合中的所有元素, 使其恢复为空集合.
     */
    public void clear() {
        users.clear();
    }
}


package dev.dong4j.zeka.stack.idea.plugin.repairer.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 违规项缓存类
 * <p> 用于在项目级别缓存代码违规项 (CodeViolation), 支持通过 Project 获取实例, 并提供获取与设置所有违规项的方法. 该类为不可变设计, 通过 volatile 保证线程安全, 适用于 IDE 或代码分析工具中缓存当前项目中的代码违规信息.
 * <p> 通过 {@code @Service(Service.Level.PROJECT)} 注解注册为项目级服务, 可通过 {@code Project.getService(ViolationCache.class)} 获取单例实例.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
@Service(Service.Level.PROJECT)
public final class ViolationCache {
    /**
     * 当前项目的所有代码违规记录
     * <p> 使用 volatile 保证多线程可见性, 初始值为空列表 </p>
     *
     * @see CodeViolation
     */
    private volatile List<CodeViolation> violations = Collections.emptyList();
    private final CopyOnWriteArrayList<ViolationCacheListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 获取指定项目的违规模型缓存实例
     * <p> 通过项目的服务获取机制获取对应项目的 ViolationCache 服务实例
     *
     * @param project 项目实例
     * @return 该项目的违规模型缓存实例
     */
    public static ViolationCache getInstance(Project project) {
        return project.getService(ViolationCache.class);
    }

    /**
     * 获取所有代码违规项列表
     * <p> 返回当前缓存中存储的所有代码违规项, 若未设置则返回空列表
     *
     * @return 所有代码违规项的不可变列表, 若无违规项则返回空列表
     */
    public List<CodeViolation> getAll() {
        return violations;
    }

    /**
     * 设置所有代码违规列表
     * <p> 将缓存中的违规列表替换为指定列表. 此方法会创建传入列表的防御性副本,
     * 以防止外部列表的修改影响缓存数据. 如果传入 null, 则设置为空列表.</p>
     *
     * @param violations 要设置的代码违规列表,null 值会被转换为空列表
     */
    public void setAll(List<CodeViolation> violations) {
        this.violations = violations == null ? Collections.emptyList() : List.copyOf(violations);
        notifyListeners();
    }

    /**
     * Register a listener for cache updates.
     *
     * @param listener listener to add
     */
    public void addListener(ViolationCacheListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    /**
     * Remove a previously registered listener.
     *
     * @param listener listener to remove
     */
    public void removeListener(ViolationCacheListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (ViolationCacheListener listener : listeners) {
            listener.violationsUpdated(violations);
        }
    }
}

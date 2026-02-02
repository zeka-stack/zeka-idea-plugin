package dev.dong4j.zeka.stack.idea.plugin.repairer.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Cache for last scanned report paths.
 */
@Getter
@Service(Service.Level.PROJECT)
public final class ReportPathCache {
    /** Checkstyle 报告路径列表, 用于缓存上次扫描的路径信息 */
    private volatile List<String> checkstylePaths = Collections.emptyList();
    /**
     * PMD 检查报告路径的缓存列表
     * <p> 用于存储最近扫描的 PMD 报告路径, 避免重复扫描
     */
    private volatile List<String> pmdPaths = Collections.emptyList();

    /**
     * 获取指定项目的 ReportPathCache 实例
     * <p> 通过项目服务机制获取当前项目对应的 ReportPathCache 单例对象
     *
     * @param project 当前项目对象
     * @return ReportPathCache 实例
     */
    public static ReportPathCache getInstance(Project project) {
        return project.getService(ReportPathCache.class);
    }

    /**
     * 更新检查路径缓存
     * <p> 根据传入的 Checkstyle 和 PMD 路径列表, 更新内部缓存. 若传入的路径列表为 null, 则使用空列表替代.
     *
     * @param checkstylePaths Checkstyle 检查路径列表, 可为 null
     * @param pmdPaths        PMD 检查路径列表, 可为 null
     */
    public void update(List<String> checkstylePaths, List<String> pmdPaths) {
        this.checkstylePaths = checkstylePaths == null ? Collections.emptyList() : List.copyOf(checkstylePaths);
        this.pmdPaths = pmdPaths == null ? Collections.emptyList() : List.copyOf(pmdPaths);
    }
}

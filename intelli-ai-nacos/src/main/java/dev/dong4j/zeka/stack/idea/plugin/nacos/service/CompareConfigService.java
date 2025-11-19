package dev.dong4j.zeka.stack.idea.plugin.nacos.service;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * 配置对比服务
 * 提供配置文件对比功能
 *
 * @author dong4j
 * @since 1.0.0
 */
@State(
    name = "CompareConfigService",
    storages = @Storage("zeka.stack.nacos.compare.xml")
)
@Service(Service.Level.PROJECT)
public final class CompareConfigService {

    /**
     * 对比本地和远程配置
     *
     * @param project       项目实例
     * @param localContent  本地配置内容
     * @param remoteContent 远程配置内容
     * @param title         对比窗口标题
     */
    public void compareConfigurations(@NotNull Project project,
                                      @NotNull String localContent,
                                      @NotNull String remoteContent,
                                      @NotNull String title) {
        // 创建 Diff 内容
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        DiffContent localDiffContent = contentFactory.create(project, localContent);
        DiffContent remoteDiffContent = contentFactory.create(project, remoteContent);

        // 创建 Diff 请求
        SimpleDiffRequest request = new SimpleDiffRequest(
            title,
            localDiffContent,
            remoteDiffContent,
            "Local Configuration",
            "Remote Configuration"
        );

        // 显示 Diff 窗口
        DiffManager.getInstance().showDiff(project, request);
    }

    /**
     * 对比配置文件
     *
     * @param project       项目实例
     * @param localFilePath 本地文件路径
     * @param remoteContent 远程配置内容
     * @param title         对比窗口标题
     */
    public void compareConfigFiles(@NotNull Project project,
                                   @NotNull String localFilePath,
                                   @NotNull String remoteContent,
                                   @NotNull String title) {
        // 创建 Diff 内容
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        DiffContent localDiffContent = contentFactory.create(project, localFilePath);
        DiffContent remoteDiffContent = contentFactory.create(project, remoteContent);

        // 创建 Diff 请求
        SimpleDiffRequest request = new SimpleDiffRequest(
            title,
            localDiffContent,
            remoteDiffContent,
            "Local File: " + localFilePath,
            "Remote Configuration"
        );

        // 显示 Diff 窗口
        DiffManager.getInstance().showDiff(project, request);
    }

    /**
     * 获取服务实例
     *
     * @param project 项目实例
     * @return 服务实例
     */
    public static CompareConfigService getInstance(@NotNull Project project) {
        return project.getService(CompareConfigService.class);
    }
}
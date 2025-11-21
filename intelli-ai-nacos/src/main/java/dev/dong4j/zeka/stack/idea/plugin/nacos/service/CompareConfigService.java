package dev.dong4j.zeka.stack.idea.plugin.nacos.service;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

import dev.dong4j.zeka.stack.idea.plugin.nacos.entity.ConfigFile;
import dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow.NacosToolWindow;

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
    private static final Key<NacosToolWindow> TOOL_WINDOW_KEY = Key.create("dev.dong4j.zeka.stack.idea.plugin.nacos.toolwindow");

    /**
     * 对比本地和远程配置（默认包含历史版本，3 面板）
     *
     * @param project       项目实例
     * @param remote        远程配置信息（必须包含 namespace、group、dataId、type）
     * @param localPath     本地文件路径（可选）
     * @param localContent  本地配置内容
     */
    public void compareConfigurations(@NotNull Project project,
                                      @NotNull ConfigFile remote,
                                      @Nullable Path localPath,
                                      @NotNull String localContent) {
        // 尝试使用 Tool Window
        NacosToolWindow toolWindow = project.getUserData(TOOL_WINDOW_KEY);
        if (toolWindow != null) {
            // 默认显示历史版本（3 面板）
            toolWindow.openCompareTab(remote, localPath, localContent, true);
            return;
        }

        // 回退到原有 Diff 窗口
        fallbackToDiffDialog(project, localContent, remote.getContent(),
                             "Compare: " + remote.getDataId(), localPath);
    }

    /**
     * 对比本地和远程配置（2 面板）- 兼容旧 API
     *
     * @param project       项目实例
     * @param localContent  本地配置内容
     * @param remoteContent 远程配置内容
     * @param title         对比窗口标题（用于提取 dataId）
     */
    @Deprecated
    public void compareConfigurations(@NotNull Project project,
                                      @NotNull String localContent,
                                      @NotNull String remoteContent,
                                      @NotNull String title) {
        // 尝试使用 Tool Window
        NacosToolWindow toolWindow = project.getUserData(TOOL_WINDOW_KEY);
        if (toolWindow != null) {
            // 创建 ConfigFile 对象（从 title 提取 dataId）
            ConfigFile remote = new ConfigFile();
            remote.setContent(remoteContent);
            remote.setDataId(title);
            remote.setNamespace("public");
            remote.setGroup("DEFAULT_GROUP");
            remote.setType("yaml");
            // 默认显示历史版本（3 面板）
            toolWindow.openCompareTab(remote, null, localContent, true);
            return;
        }

        // 回退到原有 Diff 窗口
        fallbackToDiffDialog(project, localContent, remoteContent, title, null);
    }

    /**
     * 对比本地和远程配置（带历史版本，3 面板）
     *
     * @param project      项目实例
     * @param remote       远程配置信息
     * @param localPath    本地文件路径（可选）
     * @param localContent 本地配置内容
     */
    public void compareConfigurationsWithHistory(@NotNull Project project,
                                                 @NotNull ConfigFile remote,
                                                 @Nullable Path localPath,
                                                 @NotNull String localContent) {
        // 尝试使用 Tool Window
        NacosToolWindow toolWindow = project.getUserData(TOOL_WINDOW_KEY);
        if (toolWindow != null) {
            toolWindow.openCompareTab(remote, localPath, localContent, true);
            return;
        }

        // 回退到 2 面板对比
        fallbackToDiffDialog(project, localContent, remote.getContent(),
                             "Compare: " + remote.getDataId(), localPath);
    }

    /**
     * 回退到原有 Diff 对话框
     */
    private void fallbackToDiffDialog(@NotNull Project project,
                                      @NotNull String localContent,
                                      @NotNull String remoteContent,
                                      @NotNull String title,
                                      @Nullable Path localPath) {
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        DiffContent localDiffContent;
        if (localPath != null) {
            localDiffContent = contentFactory.create(project, localPath.toString());
        } else {
            localDiffContent = contentFactory.create(project, localContent);
        }
        DiffContent remoteDiffContent = contentFactory.create(project, remoteContent);

        SimpleDiffRequest request = new SimpleDiffRequest(
            title,
            localDiffContent,
            remoteDiffContent,
            "Local Configuration",
            "Remote Configuration"
        );

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
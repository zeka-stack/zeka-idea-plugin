package dev.dong4j.zeka.stack.idea.plugin.changelog.context;

import com.intellij.diff.fragments.LineFragment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 语言上下文解析器 */
public interface LanguageContextResolver {
    /**
     * 判断是否支持该文件
     *
     * @param file 文件
     * @return 是否支持
     */
    boolean supports(@NotNull VirtualFile file);

    /**
     * 解析变更位置的上下文信息
     *
     * @param file          文件
     * @param preferredLine 优先使用的行号
     * @param fallbackLine  备用行号
     * @return 上下文信息
     */
    @Nullable
    String resolveContext(@NotNull VirtualFile file, int preferredLine, int fallbackLine);

    /**
     * 解析文件的主要符号名称
     *
     * @param project 项目
     * @param file    文件
     * @return 主要符号名称
     */
    @Nullable
    String resolvePrimarySymbolName(@NotNull Project project, @NotNull VirtualFile file);

    /**
     * 解析基于 PSI 的语义摘要信息
     * <p> 用于在 diff 基础上补充结构级语义（接口变更/实现调整/行为变化等），不负责构建 diff 本身
     *
     * @param project       项目
     * @param file          文件
     * @param beforeContent 变更前内容
     * @param afterContent  变更后内容
     * @param fragments     diff 行片段
     * @return 语义摘要文本，若无法解析则返回 null
     */
    default @Nullable String resolveSemanticSummary(@NotNull Project project,
                                                    @NotNull VirtualFile file,
                                                    @NotNull String beforeContent,
                                                    @NotNull String afterContent,
                                                    @NotNull java.util.List<LineFragment> fragments) {
        return null;
    }
}

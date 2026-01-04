package dev.dong4j.zeka.stack.idea.plugin.changelog.context;

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
}

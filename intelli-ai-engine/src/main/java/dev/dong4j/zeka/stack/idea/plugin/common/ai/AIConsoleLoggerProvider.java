package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AI 控制台日志提供者接口
 * <p>
 * 定义获取 AI 控制台日志记录器的方法, 用于为指定项目提供控制台日志记录功能
 * 该接口主要用于获取与特定项目关联的 AI 控制台日志记录器实例
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public interface AIConsoleLoggerProvider {
    /**
     * 获取控制台日志记录器
     * <p>
     * 根据项目对象返回对应的控制台日志记录器实例。
     * 如果插件没有实现控制台日志功能，可以返回 null。
     *
     * @param project 项目对象
     * @return 控制台日志记录器实例，如果没有实现则返回 null
     */
    @Nullable
    AIConsoleLogger getConsoleLogger(@NotNull Project project);
}


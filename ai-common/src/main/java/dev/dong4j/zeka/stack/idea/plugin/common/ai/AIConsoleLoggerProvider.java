package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AI 控制台日志提供者接口
 * <p>
 * 用于让插件注册自己的控制台日志实现，供 ai-common 使用。
 * 插件需要实现此接口，并在 plugin.xml 中注册为扩展点。
 *
 * @author dong4j
 * @version 1.0.0
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


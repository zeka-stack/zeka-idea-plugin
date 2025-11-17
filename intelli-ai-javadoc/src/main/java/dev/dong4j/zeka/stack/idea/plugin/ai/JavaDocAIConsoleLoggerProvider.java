package dev.dong4j.zeka.stack.idea.plugin.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLoggerProvider;
import dev.dong4j.zeka.stack.idea.plugin.console.JavaDocConsoleView;

/**
 * IntelliAI JavaDoc 控制台日志提供者
 * <p>
 * 实现 AIConsoleLoggerProvider 接口，为 intelli-ai-engine 提供控制台日志记录器。
 * 将 JavaDocConsoleView 作为控制台日志记录器返回。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class JavaDocAIConsoleLoggerProvider implements AIConsoleLoggerProvider {

    /**
     * 获取控制台日志记录器
     * <p>
     * 返回 JavaDocConsoleView 实例作为控制台日志记录器。
     *
     * @param project 项目对象
     * @return JavaDocConsoleView 实例，实现了 AIConsoleLogger 接口
     */
    @Override
    @Nullable
    public AIConsoleLogger getConsoleLogger(@NotNull Project project) {
        try {
            return JavaDocConsoleView.getInstance(project);
        } catch (Exception e) {
            // 如果获取失败，返回 null，避免影响主功能
            return null;
        }
    }
}


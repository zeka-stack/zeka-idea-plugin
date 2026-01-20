package dev.dong4j.zeka.stack.idea.plugin.repairer.maven;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * StaticAnalysisMavenStartupActivity
 * <p> 该类在项目启动时执行, 负责为 Maven 项目注册静态分析监听器, 确保在项目加载时能够捕获并报告构建相关的错误和警告.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since x.x.x
 */
public class StaticAnalysisMavenStartupActivity implements ProjectActivity {
    /**
     * 执行静态分析 Maven 启动活动
     * <p> 该方法用于执行静态分析相关的 Maven 启动操作, 注册报告监听器.
     *
     * @param project      项目对象, 表示当前处理的 Maven 项目
     * @param continuation 继续执行的上下文对象, 用于支持协程的异步执行
     * @return 返回 Unit.INSTANCE, 表示操作完成
     */
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        MavenReportListener.register(project);
        return Unit.INSTANCE;
    }
}

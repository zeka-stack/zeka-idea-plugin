package dev.dong4j.zeka.stack.idea.plugin.common.agent;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.IntelliAgentSettings;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地 Agent 应用生命周期监听器
 * <p>
 * 用于监听项目生命周期事件, 并在项目关闭时处理与本地 Agent 相关的清理逻辑.
 * 该类实现了 ProjectActivity 和 ProjectManagerListener 接口, 确保在项目启动和关闭时执行相应的操作.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.11
 * @since 1.0.0
 */
@Slf4j
public class AgentServerLifecycleListener implements ProjectActivity, AppLifecycleListener {
    /**
     * 用于记录日志的 Logger 对象
     * <p> 该 Logger 对象被初始化为 AgentServerLifecycleListener 类的实例, 用于在项目生命周期中记录相关日志信息.
     */
    private final AtomicBoolean hasRun = new AtomicBoolean(false);

    /**
     * 在项目启动时自动启动 IntelliAI Agent
     * <p> 当项目启动时, 检查配置是否允许自动启动, 并且 IntelliAI Agent 的 jar 文件是否存在. 如果满足条件, 则启动 IntelliAI Agent.
     *
     * @param project      当前项目
     * @param continuation 继续执行的延续对象
     * @return 如果是默认项目, 返回 Unit.INSTANCE; 否则返回 null
     * @since 1.0.0
     */
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (project.isDefault()) {
            return Unit.INSTANCE;
        }

        // 只在第一次运行时检查更新
        if (!hasRun.compareAndSet(false, true) || ApplicationManager.getApplication().isUnitTestMode()) {
            return Unit.INSTANCE;
        }

        try {
            AIProviderSettings settings = AIProviderSettings.getInstance();
            IntelliAgentSettings intelliAgentSettings = settings.intelliAgentSettings;
            if (intelliAgentSettings != null && intelliAgentSettings.autoStart) {
                IntelliAgentManager manager = IntelliAgentManager.getInstance();
                // 检查 jar 文件是否存在
                if (Files.exists(manager.resolveJarPath(intelliAgentSettings))) {
                    log.debug("自动启动 IntelliAI Agent（应用启动时）");
                    manager.startAgent(intelliAgentSettings);
                } else {
                    log.debug("IntelliAI Agent jar 文件不存在，跳过自动启动");
                }
            }
        } catch (Exception e) {
            log.debug("自动启动 IntelliAI Agent 失败", e);
        }
        return Unit.INSTANCE;
    }


    /**
     * 应用关闭时执行的操作
     * <p> 在应用关闭时, 停止 IntelliAI Agent. 如果 IntelliAI Agent 正在运行, 则执行停止操作.
     */
    @Override
    public void appClosing() {
        IntelliAgentManager manager = IntelliAgentManager.getInstance();
        if (manager.isRunning()) {
            manager.stopAgent();
        }
    }
}


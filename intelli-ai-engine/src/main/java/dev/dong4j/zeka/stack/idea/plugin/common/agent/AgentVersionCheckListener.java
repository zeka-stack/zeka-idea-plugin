package dev.dong4j.zeka.stack.idea.plugin.common.agent;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.IntelliAgentSettings;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;

/**
 * 代理版本检查监听器类
 * <p> 实现了 ProjectActivity 和 AppLifecycleListener 接口, 用于监听项目活动和应用程序生命周期事件.
 * 在项目启动时, 检查是否需要自动更新 IntelliAI Agent, 并在应用程序关闭时停止 IntelliAI Agent 和更新检查器.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.25
 * @since 1.0.0
 */
@Slf4j
public class AgentVersionCheckListener implements ProjectActivity, AppLifecycleListener {
    /**
     * 记录日志的 Logger 实例
     * <p>
     * 用于记录 AgentVersionCheckListener 类中的各种日志信息, 帮助调试和监控.
     */
    private final AtomicBoolean hasRun = new AtomicBoolean(false);

    /**
     * 在项目打开时启动 agent 检查更新定时器
     * <p> 此方法在项目打开时执行, 用于检查是否需要启动 IntelliAI Agent 的更新检查器. 如果项目是默认项目, 则直接返回.
     * 如果设置了自动更新, 并且 IntelliAI Agent 更新检查器尚未启动, 则启动更新检查器.
     *
     * @param project      当前项目对象
     * @param continuation 继续执行的延续对象
     * @return 如果项目是默认项目或操作成功, 则返回 Unit.INSTANCE
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
            // 启动自动更新检查器
            if (intelliAgentSettings != null && intelliAgentSettings.autoUpdate) {
                IntelliAgentUpdateChecker updateChecker = IntelliAgentUpdateChecker.getInstance();
                updateChecker.start();
            }
        } catch (Exception e) {
            log.debug("自动启动 IntelliAI Agent 失败", e);
        }
        return Unit.INSTANCE;
    }

    /**
     * 应用关闭时执行的操作
     * <p> 在应用关闭时, 停止 IntelliAI Agent 和更新检查器. 如果 IntelliAI Agent 正在运行, 则执行停止操作.
     */
    @Override
    public void appClosing() {
        try {
            log.debug("dispose() 方法被调用：停止 IntelliAI Agent 和更新检查器");

            // 停止更新检查器
            IntelliAgentUpdateChecker updateChecker = IntelliAgentUpdateChecker.getInstance();
            updateChecker.stop();

            // 停止 Agent
            IntelliAgentManager manager = IntelliAgentManager.getInstance();
            if (manager.isRunning()) {
                log.debug("检测到 IntelliAI Agent 正在运行，执行停止操作");
                manager.stopAgent();
                log.debug("IntelliAI Agent 已停止");
            } else {
                log.debug("IntelliAI Agent 未运行，无需停止");
            }
        } catch (Exception e) {
            log.debug("停止 IntelliAI Agent 失败", e);
        }
    }
}


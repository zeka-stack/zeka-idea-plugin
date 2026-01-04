package dev.dong4j.zeka.stack.idea.plugin.common.support;

import com.intellij.ide.util.RunOnceUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.concurrency.AppExecutorUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.plugin.common.util.Notifications;
import dev.dong4j.zeka.stack.idea.plugin.common.util.Urls;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * 支持启动活动类
 * <p> 实现项目启动时的初始化逻辑, 确保在特定条件下执行一次启动任务
 * <p> 通过检查项目是否为默认项目或单元测试模式来决定是否执行启动任务
 * <p> 使用原子布尔变量 `hasRun` 来确保启动任务只被执行一次
 * <p> 启动任务包括验证 URL 可达性和显示欢迎通知
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public class SupportStartupActivity implements ProjectActivity {

    /**
     * 表示活动是否已执行过的标志
     * <p>
     * 使用 `AtomicBoolean` 来确保线程安全地检查和设置活动的执行状态.
     */
    private final AtomicBoolean hasRun = new AtomicBoolean(false);

    /**
     * 执行启动活动的任务
     * <p> 在项目启动时检查是否为默认项目或单元测试模式, 如果不是, 则确保活动只运行一次, 并在延迟 5 秒后显示欢迎通知
     *
     * @param project      当前项目对象, 不能为空
     * @param continuation 异步执行的延续对象, 不能为空
     * @return 如果为默认项目或处于单元测试模式, 或者活动已运行过, 返回 null; 否则返回 Unit.INSTANCE
     */
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 只在非默认项目且非单元测试模式下运行
        if (project.isDefault() || ApplicationManager.getApplication().isUnitTestMode()) {
            return Unit.INSTANCE;
        }

        // 只在第一次运行时显示通知
        if (!hasRun.compareAndSet(false, true)) {
            return Unit.INSTANCE;
        }

        // 验证网络可达性
        Urls.verifyReachable();

        // 延迟显示通知，避免影响启动速度
        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            if (!project.isDisposed()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // 是应用第一次启动时执行, 后续启动时不执行(也就是只执行一次, 通过 id 进行唯一约束)
                    RunOnceUtil.runOnceForApp("intelli-ai-engine.2025.3.1", () -> {
                        // 在应用启动时执行的初始化逻辑
                        if (!project.isDisposed()) {
                            Notifications.showWelcomeNotification(project);
                        }
                    });
                });
            }
        }, 5000L, TimeUnit.MILLISECONDS);

        return Unit.INSTANCE;
    }
}


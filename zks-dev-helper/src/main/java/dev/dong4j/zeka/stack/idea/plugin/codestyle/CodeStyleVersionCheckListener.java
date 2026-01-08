package dev.dong4j.zeka.stack.idea.plugin.codestyle;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.plugin.settings.state.CodeStyleSettingsState;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * Code Style Version Check Listener
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-02 18:30:17
 * @since hello.world
 */
public class CodeStyleVersionCheckListener implements ProjectActivity, AppLifecycleListener {
    /**
     * 记录日志的 Logger 实例
     * <p>
     * 用于记录 CodeStyleVersionCheckListener 类中的各种日志信息, 帮助调试和监控.
     */
    private static final Logger LOG = Logger.getInstance(CodeStyleVersionCheckListener.class);
    private final AtomicBoolean hasRun = new AtomicBoolean(false);

    /**
     * 在项目打开时启动代码样式检查更新定时器
     * <p>
     * 此方法在项目打开时执行, 用于检查是否需要启动代码样式的更新检查器. 如果项目是默认项目, 则直接返回.
     * 如果设置了自动更新, 并且代码样式更新检查器尚未启动, 则启动更新检查器.
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
            CodeStyleSettingsState settings = CodeStyleSettingsState.getInstance();
            CodeStyleSettingsState.CodeStyleUpdateSettings updateSettings = settings.getCodeStyleUpdateSettings();
            // 启动自动更新检查器
            if (updateSettings != null && updateSettings.isAutoUpdate()) {
                CodeStyleUpdateChecker updateChecker = CodeStyleUpdateChecker.getInstance();
                updateChecker.start();
            }
        } catch (Exception e) {
            LOG.debug("自动启动代码样式更新检查器失败", e);
        }
        return Unit.INSTANCE;
    }

    /**
     * 应用关闭时执行的操作
     * <p>
     * 在应用关闭时, 停止代码样式更新检查器.
     */
    @Override
    public void appClosing() {
        try {
            LOG.debug("appClosing() 方法被调用：停止代码样式更新检查器");

            // 停止更新检查器
            CodeStyleUpdateChecker updateChecker = CodeStyleUpdateChecker.getInstance();
            updateChecker.stop();

            LOG.debug("代码样式更新检查器已停止");
        } catch (Exception e) {
            LOG.debug("停止代码样式更新检查器失败", e);
        }
    }
}


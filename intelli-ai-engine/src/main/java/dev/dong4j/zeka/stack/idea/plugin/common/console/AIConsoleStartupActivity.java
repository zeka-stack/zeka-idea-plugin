package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * AI Console Startup Activity
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-03 17:19:14
 * @since hello.world
 */
public class AIConsoleStartupActivity implements ProjectActivity {

    /**
     * 执行项目启动相关操作
     * <p> 当项目为默认项目或处于单元测试模式时, 直接返回空值; 否则在主线程中确保 AI 控制台标签页已注册
     *
     * @param project 当前项目对象, 非空
     * @param continuation 继续执行的回调对象, 非空
     * @return 执行结果, 始终返回 {@code Unit.INSTANCE}
     */
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (project.isDefault() || ApplicationManager.getApplication().isUnitTestMode()) {
            return Unit.INSTANCE;
        }
        ApplicationManager.getApplication().invokeLater(() ->
                                                            AIConsoleView.getInstance(project).ensureTabRegistered()
                                                       );
        return Unit.INSTANCE;
    }
}

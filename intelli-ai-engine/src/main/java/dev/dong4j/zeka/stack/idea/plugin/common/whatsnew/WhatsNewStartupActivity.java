package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.concurrency.EdtScheduledExecutorService;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * 新特性启动活动类
 * <p> 该类实现了 ProjectActivity 接口, 用于在项目启动时执行特定的操作. 具体来说, 当项目不是默认项目时,
 * 在延迟 1100 毫秒后打开新特性编辑器.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public class WhatsNewStartupActivity implements ProjectActivity {
    /**
     * 执行启动活动的逻辑, 用于在项目启动时显示新功能提示
     * <p> 如果项目是默认项目, 则直接返回 Unit.INSTANCE. 否则, 延迟 1100 毫秒后检查项目是否已被释放, 若未释放则打开新功能编辑器.
     *
     * @param project      项目对象, 表示当前处理的项目
     * @param continuation 继续执行的上下文, 用于异步操作
     * @return Unit.INSTANCE 表示操作完成
     */
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (project.isDefault()) {
            return Unit.INSTANCE;
        }

        EdtScheduledExecutorService.getInstance().schedule(() -> {
            if (!project.isDisposed()) {
                WhatsNewEditorOpener.open(project);
            }
        }, 5000L, TimeUnit.MILLISECONDS);

        return Unit.INSTANCE;
    }
}

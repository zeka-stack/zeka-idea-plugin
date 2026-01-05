package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * NextEditActionInstaller 类
 * <p> 用于在项目初始化时安装自定义的编辑器操作处理器, 以替换默认的 TAB 和 ESC 操作行为.
 * <p> 该类实现了 ProjectActivity 接口, 并确保只在非默认项目和非单元测试模式下执行一次安装逻辑.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
public final class NextEditActionInstaller implements ProjectActivity {
    /**
     * 表示 NextEditActionInstaller 是否已安装的标志.
     * <p> 该字段为静态常量, 用于确保安装逻辑只执行一次.
     *
     * @see #execute(Project, Continuation)
     */
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    /**
     * 执行下一个编辑操作的安装逻辑
     * <p> 此方法在项目活动上下文中执行, 主要用于安装或更新编辑器的动作处理器.
     * <p> 具体逻辑如下:
     * <ul>
     * <li> 如果项目是默认项目或者当前处于单元测试模式, 则直接返回.</li>
     * <li> 尝试设置安装标志为已安装状态, 如果已经安装则直接返回.</li>
     * <li> 获取编辑器动作管理器实例, 并替换特定动作处理器为自定义实现.</li>
     * </ul>
     * <p> 替换的动作处理器包括:
     * <ul>
     * <li>IDEA 编辑器的制表符动作处理器被替换为 {@link NextEditAcceptActionHandler}.</li>
     * <li>IDEA 编辑器的 ESC 键动作处理器被替换为 {@link NextEditRejectActionHandler}.</li>
     * </ul>
     *
     * @param project      当前项目实例
     * @param continuation 继续执行的回调对象
     * @return 操作完成后返回的固定对象实例
     */
    @Override
    public @NotNull Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (project.isDefault() || ApplicationManager.getApplication().isUnitTestMode()) {
            return Unit.INSTANCE;
        }
        if (!INSTALLED.compareAndSet(false, true)) {
            return Unit.INSTANCE;
        }
        EditorActionManager manager = EditorActionManager.getInstance();
        EditorActionHandler tabHandler = manager.getActionHandler(IdeActions.ACTION_EDITOR_TAB);
        manager.setActionHandler(IdeActions.ACTION_EDITOR_TAB, new NextEditAcceptActionHandler(tabHandler));

        EditorActionHandler escapeHandler = manager.getActionHandler(IdeActions.ACTION_EDITOR_ESCAPE);
        manager.setActionHandler(IdeActions.ACTION_EDITOR_ESCAPE, new NextEditRejectActionHandler(escapeHandler));

        return Unit.INSTANCE;
    }
}

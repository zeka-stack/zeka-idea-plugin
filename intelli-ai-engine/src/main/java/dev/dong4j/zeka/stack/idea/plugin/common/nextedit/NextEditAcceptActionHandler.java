package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 下一个编辑接受动作处理器
 * <p> 用于处理编辑器中“接受下一个编辑建议”的动作, 当存在待接受的编辑建议时, 优先接受建议; 否则委托给原始处理器执行.
 * <p> 该处理器通常用于支持智能编辑器插件中的“接受建议”功能, 如代码补全, 重构建议等.
 * <p> 使用示例:
 * <pre>{@code
 * EditorActionHandler originalHandler = ...;
 * NextEditAcceptActionHandler handler = new NextEditAcceptActionHandler(originalHandler);
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
public final class NextEditAcceptActionHandler extends EditorActionHandler {
    /** 原始编辑器操作处理器, 用于在无建议时回退执行 */
    private final EditorActionHandler originalHandler;

    /**
     * 初始化下一个编辑接受动作处理器
     * <p> 构造函数, 用于创建一个处理“接受下一个编辑建议”动作的处理器, 该处理器会先尝试接受当前编辑器中的建议, 若无建议则委托给原始处理器执行
     *
     * @param originalHandler 原始编辑器动作处理器, 不能为 null
     */
    public NextEditAcceptActionHandler(@NotNull EditorActionHandler originalHandler) {
        super(true);
        this.originalHandler = originalHandler;
    }

    /**
     * 执行编辑接受操作
     * <p> 该方法用于处理编辑接受逻辑, 首先尝试获取编辑追踪器并接受建议, 如果失败则调用原始处理器执行操作.
     *
     * @param editor      编辑器实例, 不能为 null
     * @param caret       光标对象, 可以为 null
     * @param dataContext 数据上下文, 不能为 null
     */
    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, @NotNull DataContext dataContext) {
        NextEditTracker tracker = getTracker(editor);
        if (tracker != null && tracker.hasSuggestion()) {
            tracker.acceptSuggestion();
            return;
        }
        originalHandler.execute(editor, caret, dataContext);
    }

    /**
     * 获取编辑器中的下一个编辑跟踪器
     * <p> 根据编辑器获取关联的 NextEditTracker 实例, 如果编辑器未关联项目或服务未初始化, 则返回 null
     *
     * @param editor 编辑器实例, 不能为 null
     * @return NextEditTracker 实例, 如果不存在或未初始化则返回 null
     */
    private @Nullable NextEditTracker getTracker(@NotNull Editor editor) {
        if (editor.getProject() == null) {
            return null;
        }
        return NextEditService.getInstance(editor.getProject()).getTracker(editor);
    }
}

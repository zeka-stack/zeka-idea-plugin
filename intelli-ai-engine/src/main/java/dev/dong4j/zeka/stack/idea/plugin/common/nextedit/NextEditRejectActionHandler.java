package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NextEditRejectActionHandler 类
 * <p> 处理拒绝下一个编辑建议的操作逻辑, 继承自 EditorActionHandler.
 * <p> 该类用于在存在下一个编辑建议时取消当前的建议, 否则调用原始处理器执行默认操作.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
public final class NextEditRejectActionHandler extends EditorActionHandler {
    /**
     * 原始的编辑器操作处理器
     * <p>
     * 该字段用于存储原始的编辑器操作处理器, 以便在需要时执行其操作.
     */
    private final EditorActionHandler originalHandler;

    /**
     * 构造函数, 初始化 NextEditRejectActionHandler 对象
     * <p> 该构造函数接受一个原始的 EditorActionHandler 并将其存储为成员变量
     *
     * @param originalHandler 原始的 EditorActionHandler, 不能为 null
     */
    public NextEditRejectActionHandler(@NotNull EditorActionHandler originalHandler) {
        super(true);
        this.originalHandler = originalHandler;
    }

    /**
     * 执行编辑器操作, 若存在待处理的编辑建议则拒绝该建议, 否则委托原始处理器执行
     * <p> 当编辑器中存在未确认的编辑建议时, 此方法会先拒绝该建议, 避免冲突; 若无建议, 则调用原始处理器执行操作
     *
     * @param editor      编辑器实例, 不能为 null
     * @param caret       光标位置, 可为 null
     * @param dataContext 数据上下文, 不能为 null
     */
    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, @NotNull DataContext dataContext) {
        NextEditTracker tracker = getTracker(editor);
        if (tracker != null && tracker.hasSuggestion()) {
            tracker.rejectSuggestion();
            return;
        }
        originalHandler.execute(editor, caret, dataContext);
    }

    /**
     * 获取编辑器的下一步编辑跟踪器
     * <p> 根据编辑器获取关联的 NextEditTracker 实例, 如果编辑器未关联项目或服务未初始化, 则返回 null
     *
     * @param editor 编辑器实例, 不能为 null
     * @return 下一步编辑跟踪器实例, 如果不存在或未初始化则返回 null
     */
    private @Nullable NextEditTracker getTracker(@NotNull Editor editor) {
        if (editor.getProject() == null) {
            return null;
        }
        return NextEditService.getInstance(editor.getProject()).getTracker(editor);
    }
}

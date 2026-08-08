package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.EditorTextField;
import com.intellij.util.Alarm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.dong4j.zeka.stack.idea.plugin.changelog.hint.CommitMessageHintService;

/**
 * Conventional Commit 高亮编辑器工厂监听器。
 * <p>
 * 复用 {@link CommitMessageHintService#isCommitMessageEditor(Editor)} 的识别逻辑，
 * 当新建编辑器被识别为 Git Commit Message 编辑器时，为其挂载
 * {@link ConventionalCommitEditorSupport}，以对首行 Conventional Commit 结构进行分段高亮。
 * <p>
 * {@link EditorFactoryListener} 由平台以单例形式在 application 级别注册（见 {@code plugin.xml}
 * 中的 {@code editorFactoryListener} 扩展点），其生命周期与 IDE 进程相同；因此这里 <b>不能</b>
 * 依赖某个短生命周期对象来清理状态，必须显式维护 编辑器 → 已挂载的 {@link ConventionalCommitEditorSupport}
 * 映射表，并在 {@link #editorReleased} 中主动释放，否则 Commit 编辑器每次重新创建（例如反复打开/关闭
 * Commit 面板）都会残留一份 {@code DocumentListener} 与 {@link com.intellij.util.Alarm}，造成泄漏。
 * <p>
 * 挂载时优先将 {@link ConventionalCommitEditorSupport} 绑定到编辑器所在组件树上最近的
 * {@link Disposable}（通常是 Commit 对话框/面板本身，随对话框关闭即释放），仅当找不到这样的
 * 父级时才回退到 {@link Project}（此时必须依赖本类的显式清理，因为 Project 存活时间远长于单次
 * Commit 编辑器）。该查找逻辑与 {@code hint} 包下
 * {@code CommitMessageHintService#findParentDisposable} 的思路一致：从编辑器向上查找
 * {@link EditorTextField}，再从其父容器向上查找第一个 {@link Disposable}。
 *
 * @author dong4j
 * @since 1.0.0
 */
public final class ConventionalCommitEditorFactoryListener implements EditorFactoryListener {

    /**
     * 已挂载的编辑器 → {@link ConventionalCommitEditorSupport} 映射。
     * <p>
     * 用于在 {@link #editorReleased} 中定位并主动释放对应实例，避免依赖父级 {@link Disposable}
     * 的释放时机（尤其是回退到 {@link Project} 时，Project 可能长时间不会释放）。
     * {@link EditorFactoryListener} 的回调均在 EDT 上触发，但仍使用 {@link ConcurrentHashMap}
     * 以与代码库中同类映射（见 {@code CommitMessageHintService#hintManagers}）保持一致的防御性写法。
     */
    private final Map<Editor, ConventionalCommitEditorSupport> attachedSupports = new ConcurrentHashMap<>();

    /** 延迟二次识别用的 Alarm（Commit 面板组件树 / DATA_KEY 可能略晚于 editorCreated）。 */
    private final Alarm delayedAttachAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Project project = editor.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // 立即尝试 + 300ms 后再试一次，覆盖 DATA_KEY / 组件树尚未就绪的情况
        ApplicationManager.getApplication().invokeLater(() -> tryAttach(editor, project), project.getDisposed());
        delayedAttachAlarm.addRequest(() -> tryAttach(editor, project), 300);
    }

    /**
     * 尝试识别并挂载；已挂载则跳过。
     */
    private void tryAttach(@NotNull Editor editor, @NotNull Project project) {
        if (project.isDisposed() || editor.isDisposed()) {
            return;
        }
        if (attachedSupports.containsKey(editor)) {
            return;
        }
        if (!CommitMessageHintService.isCommitMessageEditor(editor)) {
            return;
        }

        Disposable parent = findParentDisposable(editor);
        if (parent == null) {
            parent = project;
        }

        ConventionalCommitEditorSupport support = ConventionalCommitEditorSupport.attach(editor, parent);
        if (support != null) {
            attachedSupports.put(editor, support);
        }
    }

    /**
     * 编辑器释放时的回调。
     * <p>
     * 主动查找并释放该编辑器对应的 {@link ConventionalCommitEditorSupport}（若存在），
     * 触发其 {@link ConventionalCommitEditorSupport#dispose()}，清理 {@code DocumentListener}
     * 与 {@link com.intellij.util.Alarm}。{@link Disposer#dispose(Disposable)} 是幂等的，
     * 即使该实例已随父级 {@link Disposable} 释放（例如 Commit 面板关闭触发了父级释放），
     * 重复调用也是安全的。
     *
     * @param event 编辑器工厂事件，不能为 null
     */
    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        ConventionalCommitEditorSupport support = attachedSupports.remove(event.getEditor());
        if (support != null) {
            Disposer.dispose(support);
        }
    }

    /**
     * 查找比 {@link Project} 生命周期更短的父级 {@link Disposable}。
     * <p>
     * 逻辑与 {@code CommitMessageHintService} 中查找父级 Disposable 的方式一致：从编辑器的
     * {@code contentComponent} 向上查找所属的 {@link EditorTextField}，再从其父容器继续向上查找
     * 第一个实现了 {@link Disposable} 的 Swing 组件（通常是 Commit 对话框/工具窗口面板），
     * 找到后随该组件释放自动清理，避免长期占用 {@link Project} 生命周期。
     *
     * @param editor 目标编辑器，不能为 null
     * @return 找到的父级 {@link Disposable}；未找到时返回 {@code null}
     */
    @Nullable
    private static Disposable findParentDisposable(@NotNull Editor editor) {
        Component component = editor.getContentComponent();
        while (component != null) {
            if (component instanceof EditorTextField) {
                Component parent = component.getParent();
                while (parent != null) {
                    if (parent instanceof Disposable disposable) {
                        return disposable;
                    }
                    parent = parent.getParent();
                }
                // 找到 EditorTextField 但其父链上没有 Disposable，无需继续向上找其他 EditorTextField
                return null;
            }
            component = component.getParent();
        }
        return null;
    }
}

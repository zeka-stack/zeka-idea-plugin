package dev.dong4j.zeka.stack.idea.plugin.changelog.hint;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import lombok.extern.slf4j.Slf4j;

/**
 * Commit Message Hint 编辑器工厂监听器
 * <p>
 * 当创建新的编辑器时自动检测是否为 Commit Message Editor，
 * 如果是则创建 Hint Manager。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.11
 * @since 1.0.0
 */
@Slf4j
public class CommitMessageHintEditorFactoryListener implements EditorFactoryListener {

    /**
     * 当编辑器创建时的回调处理
     *
     * @param event 编辑器创建事件，不能为 null
     */
    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Project editorProject = editor.getProject();
        if (editorProject == null || editorProject.isDisposed()) {
            return;
        }

        // 延迟检查，因为编辑器创建时组件树可能还没有完全构建
        ApplicationManager.getApplication().invokeLater(() -> {
            if (editorProject.isDisposed() || editor.isDisposed()) {
                return;
            }

            // 检查是否为 Commit Message Editor
            boolean isCommitEditor = CommitMessageHintService.isCommitMessageEditor(editor);
            log.debug("编辑器创建: isCommitMessageEditor={}, project={}", isCommitEditor, editorProject.getName());

            if (isCommitEditor) {
                CommitMessageHintService service = editorProject.getService(CommitMessageHintService.class);
                if (service != null) {
                    CommitMessageHintManager hintManager = service.getOrCreateHintManager(editor);
                    log.debug("创建 Hint Manager: {}", hintManager != null);
                    // 确保立即更新一次提示
                    if (hintManager != null) {
                        hintManager.updateHint();
                    }
                } else {
                    log.warn("无法获取 CommitMessageHintService");
                }
            }
        }, editorProject.getDisposed());
    }

    /**
     * 当编辑器释放时调用此方法
     * <p>
     * 编辑器销毁时，Hint Manager 会自动清理（通过 Disposer）
     *
     * @param event 编辑器工厂事件，不能为 null
     */
    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        // Hint Manager 会自动清理（通过 Disposer）
    }
}

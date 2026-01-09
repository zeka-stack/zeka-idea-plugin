package dev.dong4j.zeka.stack.idea.plugin.changelog.hint;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.InlayProperties;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.vcs.commit.CommitWorkflowHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import dev.dong4j.zeka.stack.idea.plugin.changelog.git.CommitMessageGenerator;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import lombok.extern.slf4j.Slf4j;

/**
 * Commit Message Inlay Hint 管理器
 * <p>
 * 负责管理 Commit Message 编辑器中的 Inlay 提示的显示和隐藏。
 * 监听光标移动和文档变化，根据条件自动显示或隐藏提示。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.11
 * @since 1.0.0
 */
@Slf4j
public class CommitMessageHintManager implements Disposable {
    /**
     * 编辑器实例
     */
    private final Editor editor;
    /**
     * 当前显示的 Inlay 提示
     */
    @Nullable
    private Inlay<?> currentInlay;
    /**
     * 光标监听器
     */
    @Nullable
    private CaretListener caretListener;
    /**
     * 文档监听器
     */
    @Nullable
    private DocumentListener documentListener;

    /**
     * 构造函数
     *
     * @param editor 编辑器实例，不能为 null
     * @param parent 父级可释放对象，不能为 null
     */
    CommitMessageHintManager(@NotNull Editor editor, @NotNull Disposable parent) {
        this.editor = editor;
        Disposer.register(parent, this);
        setupListeners();
        // 延迟初始化提示，确保编辑器完全准备好
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            if (!editor.isDisposed()) {
                updateHint();
            }
        }, ModalityState.any());
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        CaretModel caretModel = editor.getCaretModel();

        // 监听光标移动
        caretListener = new CaretListener() {
            /**
             * 当光标位置发生变化时的回调处理
             * <p> 在编辑器中光标位置改变时触发, 用于更新相关提示信息
             *
             * @param e 光标事件对象, 包含光标位置等信息, 不能为 null
             */
            @Override
            public void caretPositionChanged(@NotNull CaretEvent e) {
                updateHint();
            }
        };
        caretModel.addCaretListener(caretListener, this);

        // 监听文档内容变化
        Document document = editor.getDocument();
        documentListener = new DocumentListener() {
            /**
             * 处理文档变更事件
             * <p> 当文档内容发生变化时, 通过系统事件队列异步触发提示更新
             * <p> 该方法在文档事件发生后, 将创建一个 InvocationEvent 并将其发布到事件队列中,
             * 以便在事件线程中调用 updateHint() 方法更新界面提示.
             *
             * @param event 文档变更事件, 不能为 null
             */
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                // 延迟更新，避免频繁刷新
                // 使用 ReadAction.nonBlocking 确保线程安全
                ReadAction.nonBlocking(() -> {
                        if (editor.isDisposed()) {
                            return false;
                        }
                        // 在 read-action 中检查条件
                        return shouldShowHintInReadAction();
                    })
                    .finishOnUiThread(ModalityState.any(), shouldShow -> {
                        if (shouldShow == null || !shouldShow || editor.isDisposed()) {
                            hideHint();
                            return;
                        }
                        // 在 EDT 中检查是否有选中的文件
                        if (!hasSelectedChanges(editor.getProject())) {
                            hideHint();
                            return;
                        }
                        // 所有检查通过，更新提示
                        updateHintInEdt();
                    })
                    .submit(AppExecutorUtil.getAppExecutorService());
            }
        };
        document.addDocumentListener(documentListener, this);
    }

    /**
     * 更新提示状态
     * <p>
     * 根据当前条件决定是否显示或隐藏提示。
     * 此方法可以在任何线程中调用，会自动处理线程切换。
     */
    public void updateHint() {
        if (editor.isDisposed()) {
            return;
        }

        // 使用 ReadAction.nonBlocking 确保线程安全
        ReadAction.nonBlocking(() -> {
                if (editor.isDisposed()) {
                    return null;
                }
                // 在 read-action 中检查基本条件并获取偏移量
                boolean shouldShow = shouldShowHintInReadAction();
                if (!shouldShow) {
                    return null;
                }
                return editor.getCaretModel().getOffset();
            })
            .finishOnUiThread(ModalityState.any(), offset -> {
                if (offset == null || editor.isDisposed()) {
                    hideHint();
                    return;
                }
                // 在 EDT 中检查是否有选中的文件
                if (!hasSelectedChanges(editor.getProject())) {
                    hideHint();
                    return;
                }
                // 所有检查通过，显示提示
                updateHintInEdt(offset);
            })
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    /**
     * 在 EDT 中更新提示
     *
     * @param offset 光标偏移量
     */
    private void updateHintInEdt(int offset) {
        if (currentInlay != null) {
            // 检查位置是否变化
            int inlayOffset = currentInlay.getOffset();
            if (inlayOffset != offset) {
                hideHint();
                showHint(offset);
            }
        } else {
            showHint(offset);
        }
    }

    /**
     * 在 EDT 中更新提示（不传偏移量，需要重新获取）
     * <p>
     * 注意：虽然已经在 EDT 中，但 getOffset() 仍然需要在 read-action 中获取。
     * 这里使用 ReadAction.nonBlocking 来获取偏移量。
     */
    private void updateHintInEdt() {
        if (editor.isDisposed()) {
            return;
        }
        // getOffset() 需要在 read-action 中执行
        ReadAction.nonBlocking(() -> {
                if (editor.isDisposed()) {
                    return null;
                }
                return editor.getCaretModel().getOffset();
            })
            .finishOnUiThread(ModalityState.any(), offset -> {
                if (offset != null && !editor.isDisposed()) {
                    updateHintInEdt(offset);
                }
            })
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    /**
     * 显示提示
     *
     * @param offset 提示显示位置（光标偏移量）
     */
    private void showHint(int offset) {
        if (currentInlay != null) {
            return;
        }

        InlayProperties properties = new InlayProperties();
        properties.relatesToPrecedingText(true);
        properties.disableSoftWrapping(true);
        CommitMessageHintRenderer renderer = new CommitMessageHintRenderer(editor, this);
        currentInlay = editor.getInlayModel().addInlineElement(offset, properties, renderer);
    }

    /**
     * 隐藏提示
     */
    public void hideHint() {
        if (currentInlay != null) {
            currentInlay.dispose();
            currentInlay = null;
        }
    }

    /**
     * 检查是否应该显示提示（在 read-action 中调用）
     * <p>
     * 此方法必须在 read-action 中调用，因为它访问了 Editor 的模型。
     *
     * @return 如果应该显示提示返回 true，否则返回 false
     */
    private boolean shouldShowHintInReadAction() {
        // 检查是否启用了"使用提交消息输入作为上下文"设置
        // 这个检查不需要 read-action，因为 SettingsState 是线程安全的
        if (!SettingsState.getInstance().useCommitMessageInputAsContext) {
            log.trace("Commit Message Hint: 设置未启用");
            return false;
        }

        // 检查文档是否有内容
        // Document.getTextLength() 是线程安全的，不需要 read-action
        if (editor.getDocument().getTextLength() == 0) {
            log.trace("Commit Message Hint: 文档为空");
            return false;
        }

        // 检查文档内容是否只包含空白字符（空格、制表符等）
        // Document.getText() 需要在 read-action 中调用
        String text = editor.getDocument().getText();
        if (text.trim().isEmpty()) {
            log.trace("Commit Message Hint: 文档内容只包含空白字符");
            return false;
        }

        // 检查是否有选中文本
        // SelectionModel.hasSelection() 需要 read-action
        if (editor.getSelectionModel().hasSelection()) {
            log.trace("Commit Message Hint: 有选中文本");
            return false;
        }

        // 检查是否正在生成中
        // CommitMessageGenerator.isRunning() 是线程安全的
        Project project = editor.getProject();
        if (project != null && CommitMessageGenerator.isRunning(project)) {
            log.trace("Commit Message Hint: 正在生成中");
            return false;
        }

        // 注意：检查是否有选中的文件需要在 EDT 中执行（获取 DataContext）
        // 这部分检查在 finishOnUiThread 回调中进行

        log.trace("Commit Message Hint: 基本条件满足");
        return true;
    }

    /**
     * 检查是否有选中的提交文件
     * <p>
     * 通过 DataContext 获取 CommitWorkflowHandler，然后检查是否有选中的变更。
     * 此方法必须在 EDT 中调用，因为获取 DataContext 需要在 EDT 中执行。
     *
     * @param project 项目实例，可能为 null
     * @return 如果有选中的文件返回 true，否则返回 false
     */
    private boolean hasSelectedChanges(@Nullable Project project) {
        if (project == null || project.isDisposed()) {
            return false;
        }

        // 确保在 EDT 中执行
        if (!com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread()) {
            log.trace("Commit Message Hint: 不在 EDT 中，无法检查选中的文件");
            // 不在 EDT 中，无法安全获取 DataContext
            // 返回 true，让提示显示，后续在 Tab 键触发时会再次检查
            return true;
        }

        try {
            // 在 EDT 中获取 DataContext
            DataContext dataContext = DataManager.getInstance()
                .getDataContext(editor.getContentComponent());
            CommitWorkflowHandler commitWorkflowHandler = dataContext.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
            if (commitWorkflowHandler == null) {
                log.trace("Commit Message Hint: 无法获取 CommitWorkflowHandler");
                return false;
            }

            // 检查是否有选中的变更
            Collection<Change> changes = dev.dong4j.zeka.stack.idea.plugin.kit.CommitUtil.getSelectedChanges(commitWorkflowHandler);
            boolean hasChanges = !changes.isEmpty();
            log.trace("Commit Message Hint: 选中的文件数量: {}", changes.size());
            return hasChanges;
        } catch (Exception e) {
            log.trace("获取提交变更失败", e);
            // 如果获取失败，返回 true，避免误判（让提示显示，后续会再次检查）
            return true;
        }
    }

    /**
     * 检查是否有活跃的提示
     *
     * @return 如果有活跃提示返回 true，否则返回 false
     */
    public boolean noActiveHint() {
        return currentInlay == null;
    }

    /**
     * 释放资源
     */
    @Override
    public void dispose() {
        hideHint();
        if (caretListener != null) {
            editor.getCaretModel().removeCaretListener(caretListener);
            caretListener = null;
        }
        if (documentListener != null) {
            editor.getDocument().removeDocumentListener(documentListener);
            documentListener = null;
        }
    }
}

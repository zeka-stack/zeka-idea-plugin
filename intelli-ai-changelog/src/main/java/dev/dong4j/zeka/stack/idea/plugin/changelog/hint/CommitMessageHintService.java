package dev.dong4j.zeka.stack.idea.plugin.changelog.hint;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.intellij.ui.EditorTextField;
import com.intellij.util.messages.MessageBusConnection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * Commit Message Hint 服务
 * <p>
 * 负责管理项目中所有 Commit Message 编辑器的 Hint 提示。
 * 监听编辑器创建和销毁，自动管理 Hint 的生命周期。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.11
 * @since 1.0.0
 */
@Slf4j
public class CommitMessageHintService implements Disposable {
    /**
     * 编辑器到 Hint Manager 的映射
     */
    private final Map<Editor, CommitMessageHintManager> hintManagers = new ConcurrentHashMap<>();
    /**
     * 项目实例
     */
    private final Project project;
    /**
     * 消息总线连接
     */
    @Nullable
    private MessageBusConnection connection;

    /**
     * 构造函数
     *
     * @param project 项目实例，不能为 null
     */
    public CommitMessageHintService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 为指定的编辑器创建 Hint Manager
     * <p>
     * 如果编辑器已经是 Commit Message Editor，则创建并返回 Hint Manager。
     *
     * @param editor 编辑器实例，不能为 null
     * @return Hint Manager 实例，如果不是 Commit Message Editor 则返回 null
     */
    @Nullable
    public CommitMessageHintManager getOrCreateHintManager(@NotNull Editor editor) {
        if (!isCommitMessageEditor(editor)) {
            return null;
        }

        return hintManagers.computeIfAbsent(editor, e -> {
            Disposable parentDisposable = findParentDisposable(e);
            if (parentDisposable == null) {
                // 如果没有找到父级 Disposable，使用项目作为父级
                parentDisposable = project;
            }
            CommitMessageHintManager manager = new CommitMessageHintManager(e, parentDisposable);
            // 注册清理回调
            Disposer.register(manager, () -> hintManagers.remove(e));
            return manager;
        });
    }

    /**
     * 获取指定编辑器的 Hint Manager
     *
     * @param editor 编辑器实例，不能为 null
     * @return Hint Manager 实例，如果不存在则返回 null
     */
    @Nullable
    public CommitMessageHintManager getHintManager(@NotNull Editor editor) {
        return hintManagers.get(editor);
    }

    /**
     * 检查指定的编辑器是否是 Commit Message Editor
     *
     * @param editor 编辑器实例，不能为 null
     * @return 如果是 Commit Message Editor 返回 true，否则返回 false
     */
    public static boolean isCommitMessageEditor(@NotNull Editor editor) {
        // 方法1：检查 EditorTextField
        // EditorTextField.getEditor() 可以获取其内部的 Editor
        // 我们需要反向查找：从 Editor 的 contentComponent 向上查找 EditorTextField
        java.awt.Component contentComponent = editor.getContentComponent();
        java.awt.Component component = contentComponent;

        // 向上遍历查找 EditorTextField 或 CommitMessage 组件
        int depth = 0;
        while (component != null && depth < 15) { // 限制深度避免无限循环
            // 如果找到 CommitMessage 组件，直接返回 true
            if (component instanceof CommitMessage) {
                log.trace("找到 CommitMessage 组件: {}", component.getClass().getName());
                return true;
            }

            // 如果找到 EditorTextField，检查其父组件是否包含 CommitMessage
            if (component instanceof EditorTextField editorField) {
                // 检查这个 EditorTextField 是否属于 CommitMessage
                CommitMessageI commitMessage = getCommitMessageFromEditor(editorField);
                if (commitMessage != null) {
                    log.trace("找到 CommitMessageI: {}", commitMessage.getClass().getName());
                    return true;
                }

                // 检查 EditorTextField 的 Editor 是否是我们当前检查的 Editor
                try {
                    Editor fieldEditor = editorField.getEditor();
                    if (fieldEditor == editor) {
                        // 这是 EditorTextField 的 Editor，继续向上查找 CommitMessage
                        java.awt.Component parent = editorField.getParent();
                        while (parent != null && depth < 15) {
                            if (parent instanceof CommitMessage) {
                                log.trace("在 EditorTextField 父组件中找到 CommitMessage");
                                return true;
                            }
                            parent = parent.getParent();
                            depth++;
                        }
                    }
                } catch (Exception e) {
                    log.trace("无法获取 EditorTextField 的 Editor", e);
                }
            }

            component = component.getParent();
            depth++;
        }

        log.trace("未识别为 Commit Message Editor，组件树深度: {}", depth);
        return false;
    }

    /**
     * 从编辑器获取 EditorTextField
     *
     * @param editor 编辑器实例，不能为 null
     * @return EditorTextField 实例，如果获取失败则返回 null
     */
    @Nullable
    private static EditorTextField getEditorTextField(@NotNull Editor editor) {
        java.awt.Component component = editor.getContentComponent();
        while (component != null) {
            if (component instanceof EditorTextField) {
                return (EditorTextField) component;
            }
            component = component.getParent();
        }
        return null;
    }

    /**
     * 从 EditorTextField 获取 CommitMessageI
     *
     * @param editorField EditorTextField 实例，不能为 null
     * @return CommitMessageI 实例，如果获取失败则返回 null
     */
    @Nullable
    private static CommitMessageI getCommitMessageFromEditor(@NotNull EditorTextField editorField) {
        java.awt.Component component = editorField.getParent();
        while (component != null) {
            if (component instanceof CommitMessage commitMessage) {
                return commitMessage;
            }
            // 尝试通过反射获取
            try {
                Method method = component.getClass().getMethod("getCommitMessageControl");
                Object result = method.invoke(component);
                if (result instanceof CommitMessageI) {
                    return (CommitMessageI) result;
                }
            } catch (Exception ignored) {
                // 忽略反射异常
            }
            component = component.getParent();
        }
        return null;
    }

    /**
     * 查找父级 Disposable
     *
     * @param editor 编辑器实例，不能为 null
     * @return Disposable 实例，如果找不到则返回 null
     */
    @Nullable
    private static Disposable findParentDisposable(@NotNull Editor editor) {
        EditorTextField editorField = getEditorTextField(editor);
        if (editorField != null) {
            java.awt.Component component = editorField.getParent();
            while (component != null) {
                if (component instanceof Disposable) {
                    return (Disposable) component;
                }
                component = component.getParent();
            }
        }
        return null;
    }

    /**
     * 释放资源
     */
    @Override
    public void dispose() {
        // 清理所有 Hint Manager
        for (CommitMessageHintManager manager : hintManagers.values()) {
            Disposer.dispose(manager);
        }
        hintManagers.clear();

        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }
}

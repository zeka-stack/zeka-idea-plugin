package dev.dong4j.zeka.stack.idea.plugin.changelog.hint;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
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
import java.util.WeakHashMap;
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
     * Commit Message 编辑器识别的「负缓存」：Document → 是否为 Commit Message Editor 的兜底识别结果。
     * <p>
     * 仅缓存「兜底组件树遍历失败（{@code false}）」的结果；{@link CommitMessage#DATA_KEY} 快路径命中
     * （{@code true}）是权威结果，不写入缓存，避免污染。用于消除在 EDT 上对同一 Document 反复执行
     * 组件树遍历 + 反射（该重复操作是提交面板卡顿、出现彩虹圈的直接根因）。
     * <p>
     * 采用 {@link WeakHashMap} 使 key（Document）被 GC 后自动清除，避免泄漏；非线程安全，读写均用
     * {@code synchronized} 保护。识别失败只发生在「文档未带 DATA_KEY 且组件树无 CommitMessage」时，
     * 对普通文件该结论长期稳定，负缓存不会误伤真正命中快路径的 Commit Message 编辑器。
     */
    private static final Map<Document, Boolean> NON_COMMIT_EDITOR_CACHE = new WeakHashMap<>();
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
     * <p>
     * 优先使用平台在 Document 上设置的 {@link CommitMessage#DATA_KEY}（与 Conventional Commit 开源插件一致），
     * 该标记在 Modal / Non-modal / 2025.3+ {@code COMMIT_WORKFLOW_UI} 下都更可靠。
     * Swing 组件树遍历仅作兜底，避免新版提交面板组件层级变化导致误判。
     *
     * @param editor 编辑器实例，不能为 null
     * @return 如果是 Commit Message Editor 返回 true，否则返回 false
     */
    public static boolean isCommitMessageEditor(@NotNull Editor editor) {
        // 优先：Document user data（平台官方标记，不依赖 Swing 父链）
        Document document = editor.getDocument();
        if (document.getUserData(CommitMessage.DATA_KEY) != null) {
            log.info("识别为 Commit Message Editor：Document 上存在 CommitMessage.DATA_KEY");
            return true;
        }

        // 负缓存：同一 Document 此前已被兜底遍历判定为「非 Commit Message Editor」，
        // 直接短路返回 false，跳过昂贵的组件树遍历 + 反射。该缓存是消除提交面板卡顿的关键。
        Boolean nonCommitCached;
        synchronized (NON_COMMIT_EDITOR_CACHE) {
            nonCommitCached = NON_COMMIT_EDITOR_CACHE.get(document);
        }
        if (nonCommitCached != null && !nonCommitCached) {
            return false;
        }

        // 兜底：沿组件树查找 CommitMessage / CommitMessageI
        java.awt.Component component = editor.getContentComponent();
        int depth = 0;
        while (component != null && depth < 15) {
            if (component instanceof CommitMessage) {
                log.info("识别为 Commit Message Editor：组件树找到 CommitMessage, depth={}", depth);
                return true;
            }

            if (component instanceof EditorTextField editorField) {
                CommitMessageI commitMessage = getCommitMessageFromEditor(editorField);
                if (commitMessage != null) {
                    log.info("识别为 Commit Message Editor：找到 CommitMessageI={}",
                        commitMessage.getClass().getName());
                    return true;
                }

                try {
                    Editor fieldEditor = editorField.getEditor();
                    if (fieldEditor == editor) {
                        java.awt.Component parent = editorField.getParent();
                        while (parent != null && depth < 15) {
                            if (parent instanceof CommitMessage) {
                                log.info("识别为 Commit Message Editor：EditorTextField 父链找到 CommitMessage");
                                return true;
                            }
                            parent = parent.getParent();
                            depth++;
                        }
                    }
                } catch (Exception e) {
                    log.debug("无法获取 EditorTextField 的 Editor", e);
                }
            }

            component = component.getParent();
            depth++;
        }

        log.info("未识别为 Commit Message Editor，组件树深度: {}", depth);
        // 记录负缓存，避免后续对同一 Document 重复执行上述组件树遍历 + 反射
        synchronized (NON_COMMIT_EDITOR_CACHE) {
            NON_COMMIT_EDITOR_CACHE.put(document, false);
        }
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

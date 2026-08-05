package dev.dong4j.zeka.stack.idea.plugin.kit;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.commit.CommitWorkflowHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * 提交工具类
 * <p> 从提交工作流中提取已勾选变更, 兼容新旧 IntelliJ 平台 API.
 * <p> 自 2025.3/2026.1 起, {@code CommitWorkflowHandler} 接口不再提供 {@code getUi()},
 * 平台改为通过 {@code VcsDataKeys.COMMIT_WORKFLOW_UI} 暴露 {@code CommitWorkflowUi}.
 * 本工具优先读取该 DataKey, 再回退到旧版 {@code handler.getUi()} 反射, 最后回退到
 * {@code VcsDataKeys.CHANGES}/{@code SELECTED_CHANGES}.
 *
 * @author dong4j
 * @version 1.0.1
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.08.05
 * @since 1.0.0
 */
@Slf4j
public final class CommitUtil {

    /**
     * 新平台暴露 CommitWorkflowUi 的 DataKey 名称.
     * <p> 使用 {@link DataKey#create(String)} 而不是直接引用 {@code VcsDataKeys.COMMIT_WORKFLOW_UI},
     * 以便在编译 SDK 仍为 2024.2 时保持源码兼容.
     */
    private static final DataKey<Object> COMMIT_WORKFLOW_UI = DataKey.create("Vcs.CommitWorkflowUI");

    private CommitUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 判断当前 DataContext 是否处于提交工作流上下文
     * <p> 用于区分「提交面板」与「Git Log」等其他入口, 避免仅因 handler 为空而误判.
     *
     * @param dataContext 动作数据上下文
     * @return 若存在 CommitWorkflowUi 或 CommitWorkflowHandler 则返回 true
     */
    public static boolean hasCommitWorkflow(@NotNull DataContext dataContext) {
        return resolveCommitWorkflowUi(dataContext) != null
               || dataContext.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER) != null;
    }

    /**
     * 从动作事件中提取已勾选的提交变更
     *
     * @param e 动作事件
     * @return 变更列表, 未选中时返回空列表
     */
    @NotNull
    public static Collection<Change> getSelectedChanges(@NotNull AnActionEvent e) {
        return getSelectedChanges(e.getDataContext());
    }

    /**
     * 从 DataContext 中提取已勾选的提交变更
     * <p> 读取顺序:
     * <ol>
     *   <li>{@code Vcs.CommitWorkflowUI} (2025.3+)</li>
     *   <li>{@code CommitWorkflowHandler.getUi()} 反射 (旧版兼容)</li>
     *   <li>{@code VcsDataKeys.CHANGES}/{@code SELECTED_CHANGES} 兜底</li>
     * </ol>
     *
     * @param dataContext 动作数据上下文
     * @return 变更列表, 未选中时返回空列表
     */
    @NotNull
    public static Collection<Change> getSelectedChanges(@NotNull DataContext dataContext) {
        Object ui = resolveCommitWorkflowUi(dataContext);
        if (ui != null) {
            // UI 已解析成功时只信任 included 列表; 勾选为空应返回空, 不能回退到 CHANGES 造成误提交上下文。
            return collectChangesFromUi(ui);
        }

        // 仅在无法拿到 CommitWorkflowUi 时, 才使用传统 VCS DataKey 兜底。
        return collectChangesFromDataKeys(dataContext);
    }

    /**
     * 从 CommitWorkflowHandler 中提取已勾选变更
     * <p> 保留旧入口以兼容既有调用方; 新代码应优先使用 {@link #getSelectedChanges(DataContext)}.
     *
     * @param commitWorkflowHandler 提交工作流处理器
     * @return 变更列表, 未选中时返回空列表
     */
    @NotNull
    public static Collection<Change> getSelectedChanges(@NotNull CommitWorkflowHandler commitWorkflowHandler) {
        Object ui = invoke(commitWorkflowHandler, "getUi");
        if (ui == null) {
            // 新平台接口已移除 getUi(), 仅保留具体实现上的方法; 反射失败时给出可诊断日志。
            log.warn("CommitWorkflowHandler.getUi() unavailable on {}, use DataContext/COMMIT_WORKFLOW_UI instead",
                     commitWorkflowHandler.getClass().getName());
            return Collections.emptyList();
        }
        return collectChangesFromUi(ui);
    }

    /**
     * 解析当前上下文中的 CommitWorkflowUi
     * <p> 优先 DataKey, 再回退 handler.getUi() 反射.
     */
    @Nullable
    private static Object resolveCommitWorkflowUi(@NotNull DataContext dataContext) {
        Object ui = dataContext.getData(COMMIT_WORKFLOW_UI);
        if (ui != null) {
            return ui;
        }

        CommitWorkflowHandler handler = dataContext.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        if (handler == null) {
            return null;
        }

        ui = invoke(handler, "getUi");
        if (ui == null) {
            log.warn("CommitWorkflowHandler.getUi() unavailable on {}, and COMMIT_WORKFLOW_UI is missing",
                     handler.getClass().getName());
        }
        return ui;
    }

    /**
     * 从 CommitWorkflowUi 收集 included changes / unversioned files
     */
    @NotNull
    private static Collection<Change> collectChangesFromUi(@Nullable Object ui) {
        if (ui == null) {
            return Collections.emptyList();
        }

        List<Change> result = new ArrayList<>();
        Object changes = invoke(ui, "getIncludedChanges");
        appendChanges(result, changes);

        Object unversioned = invoke(ui, "getIncludedUnversionedFiles");
        appendUnversioned(result, unversioned);
        return result;
    }

    /**
     * 从传统 VCS DataKey 兜底收集变更
     */
    @NotNull
    private static Collection<Change> collectChangesFromDataKeys(@NotNull DataContext dataContext) {
        Set<Change> result = new LinkedHashSet<>();
        appendChanges(result, dataContext.getData(VcsDataKeys.CHANGES));
        appendChanges(result, dataContext.getData(VcsDataKeys.SELECTED_CHANGES));
        return result;
    }

    /**
     * 将候选集合中的 {@link Change} 追加到结果
     */
    private static void appendChanges(@NotNull Collection<Change> result, @Nullable Object changes) {
        if (!(changes instanceof Collection<?> items)) {
            return;
        }
        for (Object item : items) {
            if (item instanceof Change change) {
                result.add(change);
            }
        }
    }

    /**
     * 将未版本控制文件转换为 Change 后追加
     * <p> 新平台可能返回 {@link FilePath} 或 {@link VirtualFile}, 两者都需要兼容.
     */
    private static void appendUnversioned(@NotNull Collection<Change> result, @Nullable Object unversioned) {
        if (!(unversioned instanceof Collection<?> items)) {
            return;
        }
        for (Object item : items) {
            FilePath filePath = toFilePath(item);
            if (filePath == null) {
                continue;
            }
            ContentRevision revision = new CurrentContentRevision(filePath);
            result.add(new Change(null, revision));
        }
    }

    /**
     * 将未版本控制条目规范化为 FilePath
     */
    @Nullable
    private static FilePath toFilePath(@Nullable Object item) {
        if (item instanceof FilePath filePath) {
            return filePath;
        }
        if (item instanceof VirtualFile virtualFile) {
            return new LocalFilePath(virtualFile.getPath(), virtualFile.isDirectory());
        }
        return null;
    }

    /**
     * 调用目标对象的无参公开方法
     * <p> 失败时记录 warn 日志, 便于定位平台 API 变更, 不再静默吞掉异常.
     *
     * @param target     目标对象
     * @param methodName 方法名
     * @return 调用结果; 目标为空或调用失败时返回 null
     */
    @Nullable
    private static Object invoke(@Nullable Object target, @NotNull String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (NoSuchMethodException e) {
            // 平台 API 裁剪时属预期路径, 由上层决定是否升级为 warn。
            log.debug("Method missing: {}.{}()", target.getClass().getName(), methodName);
            return null;
        } catch (ReflectiveOperationException e) {
            log.warn("Reflective call failed: {}.{}()", target.getClass().getName(), methodName, e);
            return null;
        }
    }
}

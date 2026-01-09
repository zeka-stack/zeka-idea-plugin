package dev.dong4j.zeka.stack.idea.plugin.kit;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.vcs.commit.CommitWorkflowHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * 提交工具类
 * <p> 提供从提交工作流处理器中提取已选择变更项的功能, 支持版本控制变更和未版本控制文件的统一处理.
 * <p> 该工具类通过反射机制调用指定对象的方法, 获取包含变更的集合, 并将未版本控制文件转换为变更对象加入结果集.
 * <p> 主要用途: 在版本控制系统中, 用于收集用户选择的变更内容, 便于后续提交或展示.
 * <p> 使用示例:
 * <pre>{@code
 * Collection<Change> changes = CommitUtil.getSelectedChanges(commitWorkflowHandler);
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.10
 * @since 1.0.0
 */
public class CommitUtil {

    /**
     * 获取提交的文件变更
     * <p> 从给定的 CommitWorkflowHandler 中提取已选择的变更, 并返回变更列表. 如果未选择任何变更, 则返回空列表.
     *
     * @param commitWorkflowHandler 包含 VCS 数据的 CommitWorkflowHandler 对象
     * @return 文件变更列表, 如果未选择任何变更则返回空列表
     */
    @NotNull
    public static Collection<Change> getSelectedChanges(@NotNull CommitWorkflowHandler commitWorkflowHandler) {
        Object ui = invoke(commitWorkflowHandler, "getUi");
        Object changes = invoke(ui, "getIncludedChanges");
        List<Change> result = new java.util.ArrayList<>();
        if (changes instanceof Collection<?> items) {
            for (Object item : items) {
                if (item instanceof Change change) {
                    result.add(change);
                }
            }
        }

        Object unversioned = invoke(ui, "getIncludedUnversionedFiles");
        if (unversioned instanceof Collection<?> items) {
            for (Object item : items) {
                if (item instanceof FilePath filePath) {
                    ContentRevision revision = new CurrentContentRevision(filePath);
                    result.add(new Change(null, revision));
                }
            }
        }

        return result;
    }

    /**
     * 调用目标对象的指定方法
     * <p> 通过反射机制获取目标对象的指定方法并调用, 如果目标对象为 null 或者方法调用失败, 则返回 null.
     *
     * @param target     目标对象
     * @param methodName 方法名
     * @return 方法调用的结果, 如果目标对象为 null 或者方法调用失败, 则返回 null
     */
    @Nullable
    private static Object invoke(@Nullable Object target, @NotNull String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}

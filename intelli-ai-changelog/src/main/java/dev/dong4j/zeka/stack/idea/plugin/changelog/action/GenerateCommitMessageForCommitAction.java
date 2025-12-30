package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.changelog.git.CommitMessageGenerator;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import icons.ChangelogIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * 用于生成 Git 提交消息的动作类
 * <p> 该类继承自 AnAction, 并实现了自定义的更新和执行逻辑. 在更新阶段, 检查项目是否存在未提交的更改, 并根据更改的状态设置动作的可用性和可见性.
 * 在执行阶段, 调用 CommitMessageGenerator 生成提交消息, 适用于在 Git 提交页面中自动生成提交记录.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
@Slf4j
public class GenerateCommitMessageForCommitAction extends AnAction {

    /**
     * 更新动作状态
     * <p> 检查当前项目是否有代码变更, 如果有则启用按钮, 否则禁用. 此操作在后台线程中执行, 以确保不会阻塞 UI.
     *
     * @param e 动作事件
     * @since 1.0.0
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
            return;
        }

        // 设置按钮文本和图标
        e.getPresentation().setText(ChangelogBundle.message("commit.action.text"));
        e.getPresentation().setIcon(ChangelogIcons.CHANGELOG_16);

        e.getPresentation().setEnabled(true);
        e.getPresentation().setVisible(true);
    }

    /**
     * 获取更新线程
     * <p> 在后台线程中执行更新操作, 避免阻塞 UI.
     *
     * @return ActionUpdateThread.BGT 后台线程
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 执行动作
     * <p> 当用户点击按钮时, 根据代码变更生成提交记录. 首先检查项目是否存在且未被销毁, 并验证是否配置了 AI 提供商. 然后获取已提交的文件变更, 如果没有变更则记录警告日志并返回. 最后, 使用 CommitMessageGenerator
     * 根据变更生成提交信息.
     *
     * @param e 动作事件
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // 检查 AI Provider 配置
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        if (!AIProviderUtils.hasAIProvider(project, config, PluginContents.PLUGIN_NAME)) {
            return;
        }

        log.info("Git 提交页面：开始生成提交记录");
        // 获取提交的文件变更
        Collection<Change> changes = getCommittedChanges(e);
        if (changes.isEmpty()) {
            log.warn("Git 提交页面：未选择任何文件变更");
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.no.selected.changes"));
            return;
        }

        log.info("Git 提交页面：找到 {} 个文件变更", changes.size());

        // 读取提交面板的提交信息控件，用于直接写入提交记录
        Object commitMessageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);

        // 使用生成器生成提交记录
        CommitMessageGenerator generator = new CommitMessageGenerator(project);
        generator.generateForChanges(changes, commitMessageControl);
    }

    /**
     * 获取提交的文件变更
     * <p> 从给定的 AnActionEvent 中提取已选择的变更, 并返回变更列表. 如果没有选择任何变更, 则返回空列表.
     *
     * @param e 动作事件, 包含 VCS 数据
     * @return 文件变更列表, 如果未选择任何变更则返回空列表
     */
    @NotNull
    private Collection<Change> getCommittedChanges(@NotNull AnActionEvent e) {
        // 只处理提交面板中用户当前选中的变更，按优先级依次尝试
        for (DataKey<?> key : List.of(VcsDataKeys.SELECTED_CHANGES, VcsDataKeys.CHANGES)) {
            Collection<Change> changes = normalizeChanges(e.getData(key));
            if (!changes.isEmpty()) {
                return changes;
            }
        }
        // 兼容不同版本提交面板的 DataKey 命名
        Collection<Change> dialogChanges = normalizeChanges(getDataByKeyName(e, "SELECTED_CHANGES_IN_COMMIT_DIALOG"));
        if (!dialogChanges.isEmpty()) {
            return dialogChanges;
        }
        Collection<Change> toolWindowChanges = normalizeChanges(getDataByKeyName(e, "SELECTED_CHANGES_IN_COMMIT_TOOL_WINDOW"));
        if (!toolWindowChanges.isEmpty()) {
            return toolWindowChanges;
        }
        return Collections.emptyList();
    }

    @Nullable
    private static Object getDataByKeyName(@NotNull AnActionEvent e, @NotNull String fieldName) {
        try {
            Field field = VcsDataKeys.class.getField(fieldName);
            Object value = field.get(null);
            if (value instanceof DataKey<?> dataKey) {
                return e.getData(dataKey);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // 兼容不同版本 SDK，缺失字段时直接忽略
        }
        return null;
    }

    @NotNull
    private static Collection<Change> normalizeChanges(@Nullable Object data) {
        if (data == null) {
            return Collections.emptyList();
        }
        if (data instanceof Change[] changes) {
            return Arrays.asList(changes);
        }
        if (data instanceof Change change) {
            return Collections.singletonList(change);
        }
        if (data instanceof Collection<?> items) {
            List<Change> result = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof Change change) {
                    result.add(change);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}

package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.GenerateCommitMessageService;

/**
 * 用于生成 Git 提交消息的动作类
 * <p> 该类继承自 AnAction, 并实现了自定义的更新和执行逻辑. 在更新阶段, 检查项目是否存在并且未被销毁, 并设置动作的文本和图标.
 * 在执行阶段, 获取选中的变更集合, 并调用 CommitMessageGenerator 生成提交消息.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public class GenerateCommitMessageAction extends AnAction {
    /** Action ID（用于 SplitButton 获取主动作） */
    public static final String ACTION_ID = "dev.dong4j.zeka.stack.idea.plugin.changelog.action.GenerateCommitMessageAction";
    private final GenerateCommitMessageService service = GenerateCommitMessageService.getInstance();

    /**
     * 更新动作状态
     *
     * @param e 动作事件
     * @since 1.0.0
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        service.update(e);
    }

    /**
     * 获取更新线程
     *
     * @return ActionUpdateThread.BGT 后台线程
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return service.getActionUpdateThread();
    }

    /**
     * 执行动作
     *
     * @param e 动作事件
     * @since 1.0.0
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        service.actionPerformed(e);
    }
}

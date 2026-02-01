package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.SplitButtonAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.changelog.service.GenerateCommitMessageService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderSelectionActionGroupFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成提交消息分割按钮动作类
 * <p> 该类用于在 IDE 中提供一个可分割的按钮, 用于生成提交消息, 其主功能委托给 {@code GenerateCommitMessageAction}.
 * 按钮的子菜单项动态构建, 包含 AI 提供商选择组和上下文切换功能, 用于增强提交消息生成的灵活性.
 * 本类不负责请求处理, 仅作为 UI 层面的分组与委托容器, 遵循面向对象设计原则, 避免基础设施关注.
 * <p> 主要用途: 在版本控制工具栏中提供智能提交消息生成入口, 支持动态切换 AI 提供商和上下文模式.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.18
 * @since 1.0.0
 */
public class GenerateCommitMessageSplitAction extends SplitButtonAction implements DumbAware {
    /** 主按钮行为服务 */
    private final GenerateCommitMessageService service = GenerateCommitMessageService.getInstance();

    /**
     * 初始化提交消息分拆动作按钮
     * <p> 构造函数中通过传入一个 ActionGroup 实例, 该组包含主按钮行为和下拉菜单项, 主按钮行为由 {@link GenerateCommitMessageAction} 实现, 下拉菜单项由
     * {@link #buildChildren(AnActionEvent)} 方法动态构建.</p>
     *
     * @see GenerateCommitMessageAction
     * @see #buildChildren(AnActionEvent)
     */
    public GenerateCommitMessageSplitAction() {
        super(new ActionGroup() {
            /**
             * 获取动作组的子动作数组
             * <p> 重写父类方法, 通过调用 {@code buildChildren} 方法构建并返回子动作数组
             *
             * @param e 动作事件对象, 可能为 null
             * @return 非空的动作数组, 包含当前动作组的所有子动作
             */
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                return buildChildren(e);
            }
        });
    }

    /**
     * 更新操作事件的显示状态
     * <p> 调用父类的 update 方法后, 再调用委托对象的 update 方法以同步状态 </p>
     *
     * @param e 操作事件, 不能为空
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        service.update(e);
    }

    /**
     * 获取动作更新线程的执行上下文
     * <p> 该方法委托给内部的 delegate 对象, 返回其 {@code getActionUpdateThread()} 方法的执行结果 </p>
     *
     * @return 动作更新线程的执行上下文, 类型为 {@code ActionUpdateThread}
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return service.getActionUpdateThread();
    }

    /**
     * 获取主操作项
     * <p> 返回委托的操作项, 该操作项的行为与 {@link GenerateCommitMessageAction} 一致.</p>
     *
     * @param e 操作事件, 不能为空
     * @return 主操作项, 类型为 {@code AnAction}
     */
    @Override
    protected @NotNull AnAction getMainAction(@NotNull AnActionEvent e) {
        AnAction action = ActionManager.getInstance().getAction(GenerateCommitMessageAction.ACTION_ID);
        return java.util.Objects.requireNonNull(action, GenerateCommitMessageAction.ACTION_ID + " not found");
    }

    /**
     * 判断是否使用动态分组按钮
     * <p> 本方法返回 false, 表示不使用动态分组按钮, 即按钮的显示状态不随运行时上下文动态变化.</p>
     *
     * @return false, 表示不使用动态分组按钮
     */
    @Override
    protected boolean useDynamicSplitButton() {
        return false;
    }

    /**
     * 判断分组呈现是否依赖于当前选中的操作
     * <p> 该方法用于控制分组按钮的显示状态是否随当前选中操作而变化. 本实现返回 false, 表示分组呈现不依赖于选中操作.</p>
     *
     * @return false, 表示分组呈现不依赖于选中操作
     */
    @Override
    protected boolean isGroupPresentationDependsOnSelectedAction() {
        return false;
    }

    /**
     * 构建下拉菜单中的操作项数组
     * <p> 根据当前操作事件创建包含 AI 提供商选择组和上下文开关的下拉菜单项 </p>
     * <p> 若当前事件中包含有效的项目上下文, 则添加 AI 提供商选择组; 否则仅添加分隔符和上下文开关项 </p>
     * <p> 使用 {@link AIProviderSelectionActionGroupFactory#createProviderActions} 而非 createActionGroup，
     * 避免调用标记为 @ApiStatus.OverrideOnly 的 ActionGroup.getChildren(AnActionEvent)。</p>
     *
     * @param e 操作事件, 可能为 null
     * @return 包含下拉菜单操作项的数组, 类型为 {@code AnAction[]}
     * @see AIProviderSelectionActionGroupFactory#createProviderActions
     * @see Separator#getInstance
     * @see CommitMessageContextToggleAction
     */
    private static AnAction @NotNull [] buildChildren(@Nullable AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();
        Project project = e != null ? e.getProject() : null;
        if (project != null) {
            String groupTitle = ChangelogBundle.message("statusbar.provider.selection.title");
            actions.add(Separator.create(groupTitle));
            actions.addAll(AIProviderSelectionActionGroupFactory.createProviderActions(
                project,
                ChangelogBundle.message("settings.display.name"),
                ChangelogBundle.message("settings.ai.provider.selection"),
                () -> SettingsState.getInstance().providerConfig,
                GenerateCommitMessageSplitAction::updateProviderSelection
            ));
        }
        actions.add(Separator.getInstance());
        actions.add(new CommitMessageContextToggleAction());
        return actions.toArray(AnAction[]::new);
    }

    /**
     * 更新 AI 提供商配置
     * <p> 根据指定的提供商类型和配置更新全局设置, 包括当前会话配置和全局默认配置 </p>
     *
     * @param providerType 提供商类型, 不能为空
     * @param config       提供商配置对象, 不能为空
     * @see SettingsState
     * @see AIProviderSettings
     */
    private static void updateProviderSelection(@NotNull AIProviderType providerType, @NotNull AIProviderConfig config) {
        SettingsState settings = SettingsState.getInstance();
        settings.providerConfig = config;
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.updateDefaultProviderConfig(providerType, config);
    }

    /**
     * 提交消息上下文切换操作类
     * <p> 继承自 IntelliJ 平台的 ToggleAction, 用于在状态栏中切换提交消息输入框是否作为上下文使用. 该类不负责请求处理, 仅提供状态切换和更新逻辑, 旨在避免基础设施关注, 符合面向对象设计原则.</p>
     * <p> 通过 {@code SettingsState.getInstance().useCommitMessageInputAsContext} 控制是否启用上下文模式, 支持在后台线程更新 UI 状态.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.18
     * @since 1.0.0
     */
    private static class CommitMessageContextToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 初始化 CommitMessageContextToggleAction 实例
         * <p> 调用父类构造函数, 传入状态栏提交上下文切换的显示文本资源键
         */
        CommitMessageContextToggleAction() {
            super(ChangelogBundle.message("statusbar.commit.context.toggle"));
        }

        /**
         * 获取动作更新线程的类型
         * <p>返回该动作在后台线程 (BGT) 中更新, 确保界面响应不阻塞
         *
         * @return 动作更新线程类型, 固定为 {@code ActionUpdateThread.BGT}
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断当前动作是否被选中
         * <p> 根据全局设置 {@code SettingsState.getInstance().useCommitMessageInputAsContext} 判断提交消息输入框是否启用上下文模式 </p>
         *
         * @param e 动作事件对象, 包含当前操作的上下文信息
         * @return 如果启用上下文模式则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().useCommitMessageInputAsContext;
        }

        /**
         * 设置是否在提交信息输入框中使用上下文模式
         * <p> 该方法用于更新全局设置, 控制是否在提交信息输入框中启用上下文模式
         *
         * @param e     动作事件对象, 包含当前操作的上下文信息
         * @param state 布尔值, 表示是否启用上下文模式
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().useCommitMessageInputAsContext = state;
        }
    }
}

package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.icons.AICommonIcons;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarAdapter;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarWidget;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarWidgetModel;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;

/**
 * AI Javadoc 状态栏组件类
 * <p>
 * 继承自 AIProviderStatusBarWidget, 用于在 IDE 状态栏中显示 AI Javadoc 相关的状态信息和功能.
 * 该组件提供 AI 驱动的 Javadoc 生成和管理功能的状态展示, 通过状态栏为用户提供便捷的 AI 辅助开发体验.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AIJavadocStatusBarWidget extends AIProviderStatusBarWidget {

    /**
     * 控件唯一标识
     *
     * <p>用于在状态栏系统中唯一标记该控件，便于刷新和更新。</p>
     */
    public static final String WIDGET_ID = "dev.dong4j.zeka.stack.idea.plugin.statusbar.AIProviderStatusBarWidget";

    /** 项目对象 */
    private final Project currentProject;
    /** 状态栏适配器 */
    private final AIProviderStatusBarAdapter currentAdapter;

    /**
     * 构造状态栏控件
     *
     * @param project 当前项目
     * @since 1.0.0
     */
    public AIJavadocStatusBarWidget(@NotNull Project project) {
        super(project, AIProviderStatusBarAdapterImpl.getInstance());
        this.currentProject = project;
        this.currentAdapter = AIProviderStatusBarAdapterImpl.getInstance();
    }

    /**
     * 返回控件标识符。
     *
     * @return 控件唯一标识符
     * @since 1.0.0
     */
    @Override
    public @NotNull String ID() {
        return WIDGET_ID;
    }

    /**
     * 创建状态栏弹出菜单
     * <p>
     * 重写父类方法, 在提供商列表基础上添加快捷配置选项
     *
     * @param context 数据上下文
     * @return 弹出菜单, 如果创建失败则返回 null
     * @since 1.0.0
     */
    @Override
    protected @Nullable ListPopup createPopup(@NotNull DataContext context) {
        // 获取可用的提供商列表
        List<AIProviderConfig> providers = AIProviderStatusBarWidgetModel.buildProviderItems(currentAdapter);
        if (providers.isEmpty()) {
            currentAdapter.showErrorNotification(
                currentProject,
                currentAdapter.getMessage("statusbar.provider.switch.failed.title"),
                currentAdapter.getMessage("statusbar.provider.no.available")
                                                );
            return null;
        }

        // 创建 Action 组
        DefaultActionGroup group = new DefaultActionGroup();

        // 1. 添加提供商切换选项
        for (AIProviderConfig config : providers) {
            group.add(new SwitchProviderAction(config));
        }

        // 2. 添加分隔符
        group.add(Separator.create());

        // 3. 添加快捷配置 ToggleAction
        group.add(new GenerateForClassToggleAction());
        group.add(new GenerateForMethodToggleAction());
        group.add(new GenerateForFieldToggleAction());

        // 创建弹出菜单
        return JBPopupFactory.getInstance().createActionGroupPopup(
            JavaDocBundle.message("statusbar.provider.popup.title"),
            group,
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
                                                                  );
    }

    /**
     * 切换提供商 Action
     *
     * @since 1.0.0
     */
    private class SwitchProviderAction extends AnAction {
        private final AIProviderConfig config;

        SwitchProviderAction(AIProviderConfig config) {
            super(AIProviderStatusBarWidgetModel.getProviderModelName(config));
            this.config = config;

            // 设置图标
            if (config != null && config.providerType != null) {
                Icon providerIcon = AICommonIcons.getProviderIcon(config.providerType);
                if (providerIcon != null) {
                    getTemplatePresentation().setIcon(providerIcon);
                }
            }
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            AIProviderStatusBarWidgetModel.switchDefaultProvider(currentAdapter, config);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            // 如果是当前选中的提供商,显示选中标记
            AIProviderType currentType = currentAdapter.getCurrentProviderType();
            boolean isSelected = config != null && config.providerType == currentType;
            e.getPresentation().putClientProperty("selected", isSelected);
        }
    }

    /**
     * "为类生成文档" 切换 Action
     *
     * @since 1.0.0
     */
    private static class GenerateForClassToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        GenerateForClassToggleAction() {
            super(JavaDocBundle.message("statusbar.quick.settings.generate.for.class"));
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().generateForClass;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().generateForClass = state;
        }
    }

    /**
     * "为方法生成文档" 切换 Action
     *
     * @since 1.0.0
     */
    private static class GenerateForMethodToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        GenerateForMethodToggleAction() {
            super(JavaDocBundle.message("statusbar.quick.settings.generate.for.method"));
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().generateForMethod;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().generateForMethod = state;
        }
    }

    /**
     * "为字段生成文档" 切换 Action
     *
     * @since 1.0.0
     */
    private static class GenerateForFieldToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        GenerateForFieldToggleAction() {
            super(JavaDocBundle.message("statusbar.quick.settings.generate.for.field"));
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().generateForField;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().generateForField = state;
        }
    }
}

package dev.dong4j.zeka.stack.idea.plugin.common.statusbar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.ui.RowIcon;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.component.StatusIndicatorButton;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import icons.AICommonIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 服务商选择菜单构建器
 * <p> 提供统一的服务商选择 ActionGroup, 子插件通过回调接口接入自身设置.</p>
 *
 * @author dong4j
 * @since 1.0.0
 */
@Slf4j
public final class AIProviderSelectionActionGroupFactory {
    /** 用于标识菜单项是否被选中的状态键 */
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");
    /** 菜单中选中状态的小绿点图标 */
    private static final Icon SELECTED_DOT_ICON = new StatusDotIcon(JBUI.scale(13),
                                                                    JBUI.scale(6),
                                                                    StatusIndicatorButton.STATUS_SUCCESS);

    /**
     * 私有构造函数, 禁止外部实例化
     * <p> 该类为工具类, 通过静态方法创建菜单项, 不允许直接实例化 </p>
     */
    private AIProviderSelectionActionGroupFactory() {
        // 工厂类，禁止实例化
    }

    /**
     * 创建服务商选择 ActionGroup
     *
     * @param project               当前项目
     * @param groupTitle            菜单分组标题
     * @param settingsDisplayName   设置页名称, 用于提示
     * @param selectionActionName   设置页动作名称, 用于提示
     * @param currentConfigSupplier 当前配置读取回调
     * @param configUpdater         切换配置回调
     * @return 服务商选择 ActionGroup
     */
    public static @NotNull DefaultActionGroup createActionGroup(@NotNull Project project,
                                                                @NotNull String groupTitle,
                                                                @NotNull String settingsDisplayName,
                                                                @NotNull String selectionActionName,
                                                                @NotNull Supplier<AIProviderConfig> currentConfigSupplier,
                                                                @NotNull BiConsumer<AIProviderType, AIProviderConfig> configUpdater) {
        DefaultActionGroup group = new DefaultActionGroup(groupTitle, true);
        for (AnAction action : createProviderActions(project, settingsDisplayName, selectionActionName,
                                                     currentConfigSupplier, configUpdater)) {
            group.add(action);
        }
        return group;
    }

    /**
     * 创建服务商选择动作列表（不包装为 ActionGroup）
     * <p> 用于需要扁平化展示子动作的场景，避免调用 {@code ActionGroup.getChildren(AnActionEvent)}，
     * 该方法标记为 {@code @ApiStatus.OverrideOnly}，不得由客户端代码调用。
     *
     * @param project               当前项目
     * @param settingsDisplayName   设置页名称, 用于提示
     * @param selectionActionName   设置页动作名称, 用于提示
     * @param currentConfigSupplier 当前配置读取回调
     * @param configUpdater         切换配置回调
     * @return 服务商选择动作列表，非空
     */
    public static @NotNull List<AnAction> createProviderActions(@NotNull Project project,
                                                                @NotNull String settingsDisplayName,
                                                                @NotNull String selectionActionName,
                                                                @NotNull Supplier<AIProviderConfig> currentConfigSupplier,
                                                                @NotNull BiConsumer<AIProviderType, AIProviderConfig> configUpdater) {
        List<AnAction> actions = new ArrayList<>();
        List<AIProviderConfig> providers = AIProviderUtils.getProviders();
        for (AIProviderConfig config : providers) {
            actions.add(new SwitchProviderAction(project,
                                                 settingsDisplayName,
                                                 selectionActionName,
                                                 config,
                                                 currentConfigSupplier,
                                                 configUpdater));
        }
        return actions;
    }


    /**
     * 切换 AI 服务商动作类
     * <p> 用于在 IDE 或应用程序中提供切换当前 AI 服务商配置的功能, 支持动态更新界面显示状态和配置信息.
     * <p> 该类继承自 {@link AnAction}, 实现用户界面中“切换服务商”操作的逻辑, 包括配置校验, 写入线程执行,UI 线程更新等.
     * <p> 主要职责:
     * <ul>
     *   <li> 初始化切换动作的显示名称, 图标和配置信息 </li>
     *   <li> 在用户点击时检查配置有效性并执行切换逻辑 </li>
     *   <li> 在动作更新时动态调整界面显示状态 (如选中状态, 文本, 图标)</li>
     *   <li> 确保在后台线程中执行配置写入, 避免阻塞 UI 线程 </li>
     * </ul>
     * <p> 使用示例:
     * <pre>{@code
     * SwitchProviderAction action = new SwitchProviderAction(
     *     project,
     *     "AI 设置",
     *     "切换服务商",
     *     currentConfig,
     *     () -> currentConfigSupplier,
     *     (type, config) -> configUpdater.accept(type, config)
     * );
     * }</pre>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.08
     * @since 1.0.0
     */
    private static final class SwitchProviderAction extends AnAction {
        /** 当前操作所在的项目实例 */
        private final Project project;
        /** 显示设置名称, 用于标识当前配置项的显示标题 */
        private final String settingsDisplayName;
        /** 选择操作的显示名称 */
        private final String selectionActionName;
        /** AIProvider 配置信息, 用于存储和管理当前选中的服务商配置 */
        private final AIProviderConfig config;
        /** 当前配置的供应器, 用于获取当前 AI 服务商配置 */
        private final Supplier<AIProviderConfig> currentConfigSupplier;
        /** 配置更新器, 用于更新指定 AI 服务商类型的配置信息 */
        private final BiConsumer<AIProviderType, AIProviderConfig> configUpdater;

        /**
         * 构造函数, 初始化切换服务商的动作
         * <p> 该构造函数用于创建一个 SwitchProviderAction 实例, 设置项目, 显示名称, 选择动作名称, 配置和配置更新器
         *
         * @param project               项目实例
         * @param settingsDisplayName   设置显示名称
         * @param selectionActionName   选择动作名称
         * @param config                AI 提供商配置
         * @param currentConfigSupplier 当前配置的提供者
         * @param configUpdater         配置更新器, 用于更新 AI 提供商配置
         */
        private SwitchProviderAction(@NotNull Project project,
                                     @NotNull String settingsDisplayName,
                                     @NotNull String selectionActionName,
                                     @NotNull AIProviderConfig config,
                                     @NotNull Supplier<AIProviderConfig> currentConfigSupplier,
                                     @NotNull BiConsumer<AIProviderType, AIProviderConfig> configUpdater) {
            super(buildDisplayText(config), null, buildMenuIcon(config, false));
            this.project = project;
            this.settingsDisplayName = settingsDisplayName;
            this.selectionActionName = selectionActionName;
            this.config = config;
            this.currentConfigSupplier = currentConfigSupplier;
            this.configUpdater = configUpdater;
        }

        /**
         * 执行切换 AI 服务商的操作
         * <p> 当用户选择该操作时, 会检查项目中是否已配置对应的服务商, 若已配置则执行切换逻辑.
         * <p> 切换过程在写线程中运行, 并确保在合适的 UI 线程中更新界面状态.
         *
         * @param e ActionEvent 事件对象, 提供上下文信息
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (!AIProviderUtils.hasAIProvider(project, config, settingsDisplayName, selectionActionName)) {
                return;
            }

            Runnable switchTask = () -> {
                if (project.isDisposed()) {
                    return;
                }
                try {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        AIProviderConfig configCopy = config.copy();
                        configCopy.providerType = config.providerType;
                        configUpdater.accept(config.providerType, configCopy);
                    });
                } catch (Exception exception) {
                    log.debug("切换默认服务商失败", exception);
                }
            };

            if (ApplicationManager.getApplication().isDispatchThread()) {
                switchTask.run();
            } else {
                ApplicationManager.getApplication().invokeLater(switchTask, ModalityState.defaultModalityState());
            }
        }

        /**
         * 更新动作的显示状态
         * <p> 根据当前配置是否被选中, 设置动作的显示文本, 图标和选中状态属性
         * <p> 该方法在动作更新时被调用, 用于动态调整界面显示内容
         *
         * @param e 动作事件对象, 包含当前上下文信息
         * @since 1.0
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            boolean isSelected = isConfigSelected(currentConfigSupplier.get(), config);
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
            e.getPresentation().setText(buildDisplayText(config));
            e.getPresentation().setIcon(buildMenuIcon(config, isSelected));
        }

        /**
         * 获取此操作的更新线程类型
         * <p> 返回 {@link ActionUpdateThread#BGT}, 表示该操作的界面更新将在后台线程中执行.
         *
         * @return 更新操作所使用的线程类型
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 判断当前配置是否与目标配置选中状态一致
     * <p> 比较两个配置的提供者类型和模型名称是否完全相等, 用于判断菜单项是否应标记为选中状态 </p>
     * <p> 当且仅当两个配置的 providerType 相同且 modelName 相等时, 返回 true</p>
     *
     * @param currentConfig 当前配置, 可能为 null
     * @param targetConfig  目标配置, 可能为 null
     * @return 如果两个配置的 providerType 和 modelName 均相等, 则返回 true, 否则返回 false
     */
    private static boolean isConfigSelected(@Nullable AIProviderConfig currentConfig,
                                            @Nullable AIProviderConfig targetConfig) {
        if (currentConfig == null || targetConfig == null) {
            return false;
        }
        if (currentConfig.providerType != targetConfig.providerType) {
            return false;
        }
        return Objects.equals(currentConfig.modelName, targetConfig.modelName);
    }

    /**
     * 构建菜单项显示文本
     * <p> 根据服务商配置生成对应的菜单项显示名称, 若模型名称为空则仅显示服务商名称, 否则显示服务商名称和模型名称.
     *
     * @param config 服务商配置对象, 不能为 null
     * @return 菜单项显示文本, 格式为 "服务商名称: 模型名称" 或 "服务商名称"
     */
    private static @NotNull String buildDisplayText(@NotNull AIProviderConfig config) {
        String providerName = config.providerType != null
                              ? config.providerType.getDisplayName()
                              : "Unknown Provider";
        String modelName = config.modelName != null && !config.modelName.isEmpty()
                           ? config.modelName
                           : "";
        if (modelName.isEmpty()) {
            return providerName;
        }
        return providerName + ": " + modelName;
    }

    /**
     * 构建菜单图标
     * <p> 根据服务商配置和选中状态生成对应的菜单图标
     * <p> 如果未选中, 则直接返回服务商图标; 如果选中, 则在服务商图标前加上一个表示选中的小绿点图标
     *
     * @param config   服务商配置对象
     * @param selected 是否选中状态
     * @return 返回生成的菜单图标
     */
    private static @Nullable Icon buildMenuIcon(@NotNull AIProviderConfig config, boolean selected) {
        Icon providerIcon = getProviderIcon(config);
        if (!selected) {
            return providerIcon;
        }
        RowIcon rowIcon = new RowIcon(2);
        rowIcon.setIcon(SELECTED_DOT_ICON, 0);
        rowIcon.setIcon(providerIcon, 1);
        return rowIcon;
    }

    /**
     * 获取服务商图标
     * <p> 根据服务商配置对象获取对应的图标, 如果配置中未指定服务商类型则返回 null</p>
     *
     * @param config 服务商配置对象, 不能为 null
     * @return 服务商对应的图标, 如果未指定服务商类型则返回 null
     */
    private static @Nullable Icon getProviderIcon(@NotNull AIProviderConfig config) {
        if (config.providerType == null) {
            return null;
        }
        return AICommonIcons.getProviderIcon(config.providerType);
    }

    /**
     * 菜单选中状态的圆点图标
     *
     * @param iconSize 图标尺寸, 决定整个图标区域的宽度和高度
     * @param dotSize  圆点图标大小
     * @param color    配置图标颜色
     */
        private record StatusDotIcon(int iconSize, int dotSize, Color color) implements Icon {
            /**
             * 初始化状态圆点图标的构造方法
             * <p> 设置图标尺寸, 圆点尺寸和颜色, 用于绘制选中状态的圆点图标
             *
             * @param iconSize 图标整体尺寸
             * @param dotSize  圆点尺寸
             * @param color    圆点颜色, 不能为 null
             */
            private StatusDotIcon(int iconSize, int dotSize, @NotNull Color color) {
                this.iconSize = iconSize;
                this.dotSize = dotSize;
                this.color = color;
            }

            /**
             * 绘制菜单选中状态的圆点图标
             * <p> 该方法用于在指定位置绘制一个圆点形状的图标, 用于表示菜单项的选中状态.
             *
             * @param c 绘制所处的组件对象
             * @param g 用于绘制的图形上下文
             * @param x 图标左上角的横坐标
             * @param y 图标左上角的纵坐标
             */
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                int offset = Math.max(0, (iconSize - dotSize) / 2);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(x + offset, y + offset, dotSize, dotSize);
                g2.dispose();
            }

            /**
             * 获取图标的宽度
             * <p> 返回图标的宽度, 单位为像素
             *
             * @return 图标的宽度
             */
            @Override
            public int getIconWidth() {
                return iconSize;
            }

            /**
             * 获取图标的高度
             *
             * @return 图标高度, 单位为像素
             */
            @Override
            public int getIconHeight() {
                return iconSize;
            }
        }
}

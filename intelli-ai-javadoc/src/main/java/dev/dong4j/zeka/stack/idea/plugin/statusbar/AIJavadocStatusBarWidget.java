package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarWidget;

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

    /**
     * 构造状态栏控件
     *
     * @param project 当前项目
     * @since 1.0.0
     */
    public AIJavadocStatusBarWidget(@NotNull Project project) {
        super(project, AIProviderStatusBarAdapterImpl.getInstance());
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
}

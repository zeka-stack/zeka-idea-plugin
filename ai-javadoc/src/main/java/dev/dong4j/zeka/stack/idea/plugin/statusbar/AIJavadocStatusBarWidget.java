package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarWidget;

/**
 * 状态栏默认服务商切换控件
 * <p>
 * 这是 ai-javadoc 插件对通用状态栏组件的包装，使用适配器模式桥接插件特定的配置和资源。
 *
 * @author dong4j
 * @version 1.0.0
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

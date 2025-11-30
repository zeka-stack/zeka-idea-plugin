package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarWidgetFactory;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;

/**
 * AI JavaDoc 状态栏组件工厂类
 * <p>
 * 该类继承自 AIProviderStatusBarWidgetFactory, 用于创建和管理 AI JavaDoc 相关的状态栏组件.
 * 负责初始化状态栏组件的适配器, 组件 ID 以及显示名称, 为用户提供 AI 驱动的 JavaDoc 生成和管理功能.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AIJavadocStatusBarWidgetFactory extends AIProviderStatusBarWidgetFactory {

    /**
     * 构造工厂
     */
    public AIJavadocStatusBarWidgetFactory() {
        super(
            AIProviderStatusBarAdapterImpl.getInstance(),
            AIJavadocStatusBarWidget.WIDGET_ID,
            JavaDocBundle.message("statusbar.provider.factory.name")
             );
    }
}

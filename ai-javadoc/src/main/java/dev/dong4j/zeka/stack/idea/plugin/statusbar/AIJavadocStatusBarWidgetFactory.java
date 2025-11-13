package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarWidgetFactory;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;

/**
 * 状态栏默认服务商控件工厂
 * <p>
 * 负责创建 {@link AIJavadocStatusBarWidget} 实例并在插件启动时注册到状态栏。
 *
 * @author dong4j
 * @version 1.0.0
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

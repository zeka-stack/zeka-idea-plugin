package dev.dong4j.zeka.stack.idea.plugin.template.live;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.settings.UniformFormatSettingsState;

/**
 * Live Template 上下文
 * <p>
 * 该类用于定义一个 Live Template 上下文，适用于所有文件类型。它继承自 TemplateContextType，
 * 并覆盖了 isInContext 方法，根据配置决定是否启用 Live Template 功能。
 * 如果启用了 Live Template 功能，则该方法返回 true，表示该插件在所有上下文中都可用；
 * 如果未启用，则返回 false，表示该插件不可用。
 * 这是 ZKS Dev Helper 插件中代码样式模块的组件。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.10.25
 * @since 1.0.0
 */
public class UniformLiveTemplateContext extends TemplateContextType {

    /**
     * 构造函数，用于初始化 Live Template 上下文对象
     * <p>
     * 通过指定的名称、显示名称和模板上下文类型来创建模板上下文对象
     */
    protected UniformLiveTemplateContext() {
        super("Zeka stack");
    }

    /**
     * 判断指定文件和偏移量是否处于当前插件的上下文中
     * <p>
     * 该方法检查是否启用了 Live Template 功能，如果启用则返回 true，表示该插件在所有上下文中都可用；
     * 如果未启用则返回 false，表示该插件不可用。
     *
     * @param templateActionContext 模板操作上下文对象
     * @return 如果启用了 Live Template 功能返回 true，否则返回 false
     */
    @Override
    public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
        // 检查是否启用 Live Template 功能
        try {
            UniformFormatSettingsState settings = UniformFormatSettingsState.getInstance();
            return settings != null && settings.isEnableLiveTemplates();
        } catch (Exception e) {
            // 如果获取配置失败，默认返回 true（保持向后兼容）
            return true;
        }
    }
}

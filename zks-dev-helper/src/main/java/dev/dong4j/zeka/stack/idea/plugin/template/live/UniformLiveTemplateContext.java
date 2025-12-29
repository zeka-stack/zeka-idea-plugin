package dev.dong4j.zeka.stack.idea.plugin.template.live;

import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

/**
 * Live Template 上下文
 * <p>
 * 该类用于定义一个 Live Template 上下文，适用于所有文件类型。它继承自 TemplateContextType，
 * 并覆盖了 isInContext 方法，使其在所有上下文中都可用。
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
        super("ZEKA_STACK", "Zeka Stack", TemplateContextType.class);
    }

    /**
     * 判断指定文件和偏移量是否处于当前插件的上下文中
     * <p>
     * 该方法始终返回 true，表示该插件在所有上下文中都可用
     *
     * @param file   要检查的文件对象
     * @param offset 偏移量位置
     * @return 始终返回 true
     */
    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        return true; // 在所有上下文中都可用
    }
}

package dev.dong4j.zeka.stack.idea.javadoc.diagnostic;

import dev.dong4j.zeka.stack.idea.plugin.common.diagnostic.AbstractFeedbackContextProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Javadoc 反馈上下文提供者
 * <p> 用于标识上报来源为 IntelliAI Javadoc 插件.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2026.01.28
 * @since 1.0.0
 */
public class JavadocFeedbackContextProvider extends AbstractFeedbackContextProvider {

    /**
     * 获取标题前缀
     * <p> 重写父类方法, 返回 null 表示不提供标题前缀 </p>
     *
     * @return 标题前缀, 此处返回 null
     */
    @Override
    public @Nullable String getTitlePrefix() {
        return null;
    }
}

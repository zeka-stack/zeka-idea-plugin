package dev.dong4j.zeka.stack.idea.plugin.common.diagnostic;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 反馈上下文持有器
 * <p> 使用 ThreadLocal 保存当前反馈来源插件 ID, 供错误上报流程读取.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2026.01.28
 * @since 1.0.0
 */
public final class FeedbackContextHolder {
    /** 保存当前反馈来源插件 ID 的 ThreadLocal 变量 */
    private static final ThreadLocal<String> PLUGIN_ID = new ThreadLocal<>();

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该构造函数为私有, 确保 {@link FeedbackContextHolder} 类不能被直接实例化, 只能通过静态方法操作.</p>
     */
    private FeedbackContextHolder() {
    }

    /**
     * 获取当前线程的插件 ID
     *
     * @return 插件 ID, 可能为 null
     */
    @Nullable
    public static String getCurrentPluginId() {
        return PLUGIN_ID.get();
    }

    /**
     * 设置当前线程的插件 ID
     *
     * @param pluginId 插件 ID
     * @return 可自动恢复的 Token
     */
    @NotNull
    public static Token withPluginId(@NotNull String pluginId) {
        String previous = PLUGIN_ID.get();
        PLUGIN_ID.set(pluginId);
        return new Token(previous);
    }

    /**
     * 在当前线程未设置时写入插件 ID
     *
     * @param pluginId 插件 ID
     * @return 可自动恢复的 Token
     */
    @NotNull
    public static Token pushIfAbsent(@Nullable String pluginId) {
        String previous = PLUGIN_ID.get();
        if (StringUtil.isEmptyOrSpaces(previous) && pluginId != null) {
            PLUGIN_ID.set(pluginId);
            return new Token(previous);
        }
        return new Token(previous, false);
    }

    /**
     * 上下文恢复令牌
     */
    public static final class Token implements AutoCloseable {
        /** 上一个上下文恢复令牌值 */
        private final String previous;
        /** 是否发生过变更, 用于决定是否需要恢复上下文 */
        private final boolean changed;

        /**
         * 使用指定的前一个值创建 Token 实例
         * <p> 此构造函数会设置 changed 标志为 true, 表示上下文发生了变化
         *
         * @param previous 前一个插件 ID 值
         */
        private Token(String previous) {
            this(previous, true);
        }

        /**
         * 创建上下文恢复令牌实例
         * <p> 使用指定的先前值和变更标志初始化令牌
         *
         * @param previous 之前的上下文值
         * @param changed  标识上下文是否发生变更的标志
         */
        private Token(String previous, boolean changed) {
            this.previous = previous;
            this.changed = changed;
        }

        /**
         * 关闭当前上下文并恢复之前的插件 ID
         * <p> 如果上下文没有改变, 则不做任何操作. 如果有改变, 则根据 previous 是否为空来决定是移除插件 ID 还是恢复之前的插件 ID.
         *
         */
        @Override
        public void close() {
            if (!changed) {
                return;
            }
            if (previous == null) {
                PLUGIN_ID.remove();
            } else {
                PLUGIN_ID.set(previous);
            }
        }
    }
}

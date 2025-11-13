package dev.dong4j.zeka.stack.idea.plugin.common.util;

import com.intellij.DynamicBundle;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * AI 通用资源包。
 */
public final class AICommonBundle extends DynamicBundle {

    /**
     * 资源包名称
     * <p>
     * 用于加载国际化消息的资源文件
     *
     * @see com.intellij.l10n.Bundle
     */
    @NonNls
    private static final String BUNDLE = "messages.AICommonBundle";

    /** AICommonBundle 的单例实例 */
    private static final AICommonBundle INSTANCE = new AICommonBundle();

    /**
     * 私有构造函数, 用于初始化 AICommonBundle 类
     * <p>
     * 通过调用父类的构造函数, 传入 BUNDLE 参数进行初始化
     *
     * @since 1.0
     */
    private AICommonBundle() {
        super(BUNDLE);
    }

    /**
     * 根据指定的资源键和参数获取本地化的消息字符串
     * <p>
     * 使用给定的资源键和参数从资源包中查找并返回对应的消息字符串.
     *
     * @param key    资源键, 用于标识特定的消息
     * @param params 可变参数, 用于替换消息中的占位符
     * @return 本地化的消息字符串
     * @throws MissingResourceException 如果找不到对应的资源键
     * @since 1.0
     */
    @NotNull
    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    /**
     * 根据指定的键和参数生成一个延迟加载的消息字符串供应商
     * <p>
     * 该方法用于获取一个 Supplier 对象, 该对象在调用时会根据提供的键和参数
     * 从资源包中获取对应的消息字符串.
     *
     * @param key    消息键, 必须为非空值
     * @param params 可变参数, 用于替换消息中的占位符
     * @return 一个 Supplier 对象, 用于延迟加载消息字符串
     * @since 1.0
     */
    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}

package dev.dong4j.zeka.stack.idea.plugin.nacos.util;

import com.intellij.DynamicBundle;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * Nacos 插件国际化资源管理类
 *
 * <p>负责加载和管理插件的多语言资源文件。
 * 资源文件位置：src/main/resources/messages.properties
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class NacosBundle extends DynamicBundle {

    /** 消息资源包名称，用于加载国际化消息 */
    @NonNls
    private static final String BUNDLE = "messages";

    /**
     * 单例实例
     */
    private static final NacosBundle INSTANCE = new NacosBundle();

    /**
     * 私有构造函数
     */
    private NacosBundle() {
        super(BUNDLE);
    }

    /**
     * 获取国际化消息
     *
     * @param key    资源键，对应 messages.properties 中的键
     * @param params 格式化参数，用于替换消息中的 {0}, {1} 等占位符
     * @return 国际化后的消息字符串
     */
    @NotNull
    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    /**
     * 获取国际化消息（延迟加载）
     *
     * @param key    资源键，对应 messages.properties 中的键
     * @param params 格式化参数，用于替换消息中的 {0}, {1} 等占位符
     * @return 消息提供者，调用 get() 方法获取实际消息
     */
    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                                  Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}


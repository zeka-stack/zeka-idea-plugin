package dev.dong4j.zeka.stack.idea.plugin.repairer.util;

import com.intellij.DynamicBundle;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * 修复人员资源包类
 * <p> 用于管理与修复人员相关的国际化消息资源, 继承自动态资源包 {@code DynamicBundle},
 * 提供便捷的方法来获取本地化字符串或延迟加载的字符串供应器.
 *
 * @author dong4j
 * @version 1.0.0
 * @email mailto:dong4j@gmail.com
 * @date 2026.01.20
 * @since 1.0.0
 */
public class RepairerBundle extends DynamicBundle {

    /** 用于加载资源的资源包路径, 不进行国际化处理 */
    @NonNls
    private static final String BUNDLE = "messages.RepairerBundle";

    /** 单例实例 */
    private static final RepairerBundle INSTANCE = new RepairerBundle();

    /**
     * 初始化 RepairerBundle 实例
     * <p> 调用父类构造函数, 使用指定的资源包名称初始化动态资源绑定
     *
     */
    private RepairerBundle() {
        super(BUNDLE);
    }

    /**
     * 获取指定键的消息字符串
     * <p> 根据给定的键和参数从资源包中检索本地化消息字符串
     *
     * @param key    消息键
     * @param params 参数列表, 用于替换消息中的占位符
     * @return 本地化的消息字符串
     * @since 1.0
     */
    @NotNull
    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    /**
     * 获取延迟加载的本地化消息
     * <p> 根据指定的键和参数, 返回一个延迟加载的字符串消息. 该消息会在首次访问时进行解析和格式化.
     *
     * @param key    消息键, 必须非空且对应于资源包中的有效消息
     * @param params 可变参数列表, 用于替换消息中的占位符
     * @return 延迟加载的字符串消息
     */
    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                                  Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}

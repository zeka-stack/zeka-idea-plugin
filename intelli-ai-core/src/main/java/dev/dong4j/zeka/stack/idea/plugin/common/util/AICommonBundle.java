package dev.dong4j.zeka.stack.idea.plugin.common.util;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * AI 通用资源包类
 * <p>
 * 管理AI 相关的国际化消息资源, 提供静态方法来获取本地化消息,
 * 支持参数化消息格式化, 并提供延迟加载消息的功能.
 * <p>
 * 约束说明: 本类最初继承 IntelliJ 的 {@code DynamicBundle}, 为让 core 模块脱离 IDE 运行
 * (供独立 MCP Server 等宿主复用), 改为基于 JDK {@link ResourceBundle} 实现:
 * <ul>
 *   <li>资源文件仍为 {@code messages.AICommonBundle}, 与原 engine 版本共用同一份 properties</li>
 *   <li>locale 解析沿用 JVM 默认 locale, 与 DynamicBundle 在未定制语言的 IDE 中行为一致</li>
 *   <li>占位符替换使用 {@link MessageFormat}, 与 DynamicBundle 的消息格式化行为一致</li>
 * </ul>
 * 注意: 在 IDE 内使用时, 本类必须由插件类加载器加载(资源在插件 zip 内), 否则找不到资源文件.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public final class AICommonBundle {

    /**
     * 资源包名称
     * <p>
     * 用于加载国际化消息的资源文件
     */
    @NonNls
    private static final String BUNDLE = "messages.AICommonBundle";

    /**
     * 资源包实例
     * <p>
     * 使用当前类加载器加载, 保证在插件化环境(独立 classloader)下也能正确读取资源;
     * ResourceBundle 自带缓存, 重复调用不会重复读取文件
     */
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(
        BUNDLE, Locale.getDefault(), AICommonBundle.class.getClassLoader());

    /**
     * 私有构造函数, 防止实例化工具类
     */
    private AICommonBundle() {
    }

    /**
     * 根据指定的资源键和参数获取本地化的消息字符串
     * <p>
     * 使用给定的资源键和参数从资源包中查找并返回对应的消息字符串,
     * 当存在参数时通过 {@link MessageFormat} 替换消息中的占位符.
     *
     * @param key    资源键, 用于标识特定的消息
     * @param params 可变参数, 用于替换消息中的占位符
     * @return 本地化的消息字符串
     * @since 1.0
     */
    @NotNull
    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        String pattern = RESOURCE_BUNDLE.getString(key);
        return params.length == 0 ? pattern : MessageFormat.format(pattern, params);
    }

    /**
     * 根据指定的键和参数生成一个延迟加载的消息字符串供应商
     * <p>
     * 该方法用于获取一个 Supplier 对象, 该对象在调用时才从资源包中获取对应的消息字符串.
     * 与原 DynamicBundle#getLazyMessage 的语义一致: 延迟求值, 每次调用 get 都会重新格式化.
     *
     * @param key    消息键, 必须为非空值
     * @param params 可变参数, 用于替换消息中的占位符
     * @return 一个 Supplier 对象, 用于延迟加载消息字符串
     * @since 1.0
     */
    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                                  Object... params) {
        return () -> message(key, params);
    }
}

package dev.dong4j.zeka.stack.idea.plugin.terminal.util;

import com.intellij.DynamicBundle;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * 终端消息资源包类
 * <p> 该类继承自 {@code DynamicBundle}, 用于管理和获取终端相关的国际化消息. 通过静态方法可以方便地获取指定键的消息文本及其懒加载的供应商.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class TerminalBundle extends DynamicBundle {

    /**
     * 资源包名称
     * <p> 用于指定加载多语言资源文件的路径
     *
     * @see #INSTANCE
     */
    @NonNls
    private static final String BUNDLE = "messages.TerminalBundle";

    /**
     * 当前插件的国际化资源实例
     * <p> 用于访问插件的多语言资源文件, 支持消息获取和延迟消息加载
     *
     * @see TerminalBundle
     */
    private static final TerminalBundle INSTANCE = new TerminalBundle();

    /**
     * 初始化 TerminalBundle 实例
     * <p> 调用父类构造函数初始化资源包, 用于加载和管理多语言资源文件
     *
     * @since 1.0.0
     */
    private TerminalBundle() {
        super(BUNDLE);
    }

    /**
     * 获取国际化消息字符串
     * <p> 根据指定的键和参数获取对应的语言资源消息, 支持动态参数替换
     *
     * @param key    消息键, 不能为空
     * @param params 可变参数, 用于替换消息中的占位符, 可以为 null
     * @return 国际化消息字符串, 不能为空
     *     <p>
     *     使用示例:
     *     <pre>{@code
     *         String greeting = TerminalBundle.message("greeting", "Alice");
     *         String error = TerminalBundle.message("error.invalid", "username");
     *         }</pre>
     */
    @NotNull
    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    /**
     * 获取消息的延迟加载引用
     * <p> 返回一个 Supplier 对象, 用于延迟获取指定键的消息字符串, 支持参数占位符替换
     * <p> 使用示例:
     * <pre>{@code
     * Supplier<String> messageSupplier = TerminalBundle.messagePointer("greeting", "Alice");
     * String message = messageSupplier.get(); // 实际获取消息
     * }</pre>
     *
     * @param key    消息键, 不能为空
     * @param params 可选参数, 用于替换消息中的占位符 (如 {0}, {1})
     * @return 消息的延迟加载引用, 用于按需获取消息内容
     */
    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                                  Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}

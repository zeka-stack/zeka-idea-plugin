package dev.dong4j.zeka.stack.idea.plugin.example.util;

import com.intellij.DynamicBundle;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * 示例资源包类
 * <p> 用于管理与国际化消息相关的资源文件, 提供便捷的方法来获取本地化消息字符串.
 * <p> 该类通过单例模式确保资源包的唯一性, 并支持动态加载和消息占位符替换.
 * <p> 使用示例:
 * <pre>{@code
 * String message = ExampleBundle.message("welcome.message", "John");
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class ExampleBundle extends DynamicBundle {

    /** 资源文件名称, 用于加载多语言消息配置 */
    @NonNls
    private static final String BUNDLE = "messages.ExampleBundle";

    /**
     * 示例资源包单例实例
     * <p> 用于全局访问国际化消息资源, 避免重复创建实例
     *
     * @see ExampleBundle
     */
    private static final ExampleBundle INSTANCE = new ExampleBundle();

    /**
     * 私有构造函数, 用于初始化示例资源包
     * <p> 该构造函数被设计为私有, 确保只能通过静态方法获取实例
     * <p> 通过调用父类构造函数, 使用预定义的资源包名称进行初始化
     *
     * @see ExampleBundle#INSTANCE
     * @see DynamicBundle#DynamicBundle(String)
     */
    private ExampleBundle() {
        super(BUNDLE);
    }

    /**
     * 获取指定键的国际化消息字符串
     * <p> 从资源文件中加载并返回对应键的消息内容, 支持参数替换
     *
     * @param key    消息键, 必须存在于资源文件中
     * @param params 用于格式化消息的可变参数
     * @return 格式化后的国际化消息字符串, 不会为 null
     * @since 1.0.0
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
     * Supplier<String> message = ExampleBundle.messagePointer("greeting", "Alice");
     * String result = message.get(); // 实际获取消息内容
     * }</pre>
     *
     * @param key    消息键, 不能为空, 必须是有效的资源键
     * @param params 可选参数, 用于替换消息模板中的占位符 (如 {0}, {1})
     * @return 消息的延迟加载引用, 调用 get() 方法时才会获取实际消息内容
     */
    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                                  Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}


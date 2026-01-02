package dev.dong4j.zeka.stack.idea.plugin.swagger.util;

import com.intellij.DynamicBundle;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * Swagger 资源包类
 * <p> 提供与 Swagger 相关的国际化消息资源管理, 基于指定的资源文件进行消息加载和解析.
 * <p> 该类继承自 {@code DynamicBundle}, 并封装了静态方法用于获取本地化字符串或延迟加载的消息指针.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class SwaggerBundle extends DynamicBundle {

    /** BUNDLE 资源文件名, 用于加载多语言资源 */
    @NonNls
    private static final String BUNDLE = "messages.SwaggerBundle";

    /** SwaggerBundle 的单例实例 */
    private static final SwaggerBundle INSTANCE = new SwaggerBundle();

    /**
     * 私有构造函数
     * <p>SwaggerBundle 类的私有构造函数, 用于防止外部实例化.
     * <p> 此构造函数调用父类的构造函数, 并传入资源文件的名称.
     *
     * @since 1.0.0
     */
    private SwaggerBundle() {
        super(BUNDLE);
    }

    /**
     * 获取国际化消息字符串
     * <p> 根据指定的键和参数获取对应的语言资源消息, 支持动态参数替换
     * <p> 使用示例:
     * <pre>{@code
     * String greeting = SwaggerBundle.message("greeting.welcome", "John");
     * String error = SwaggerBundle.message("error.not_found", 404);
     * }</pre>
     *
     * @param key    消息键, 不能为空
     * @param params 可变参数, 用于消息中的占位符替换, 可以为零个或多个
     * @return 国际化消息字符串, 不能为空
     */
    @NotNull
    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    /**
     * 获取延迟加载的国际化消息
     * <p> 根据指定的键和参数返回一个延迟加载的消息字符串, 适用于需要按需加载资源的情况.
     *
     * @param key    国际化消息的键, 不能为空
     * @param params 可变参数列表, 用于替换消息中的占位符
     * @return 延迟加载的消息字符串
     */
    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                                  Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}

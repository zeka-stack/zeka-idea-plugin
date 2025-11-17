package dev.dong4j.zeka.stack.idea.plugin.util;

import com.intellij.DynamicBundle;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * 国际化资源管理类
 *
 * <p>负责加载和管理插件的多语言资源文件。
 * 资源文件位置：src/main/resources/messages.properties
 *
 * <p>作为插件国际化支持的核心组件，提供统一的消息访问接口，
 * 支持运行时动态切换语言环境。
 *
 * <p>支持的语言：
 * <ul>
 *   <li>英文（默认）- messages.properties</li>
 *   <li>简体中文 - messages_zh_CN.properties</li>
 * </ul>
 *
 * <p>设计模式：
 * <ul>
 *   <li>单例模式：通过 INSTANCE 提供全局唯一实例</li>
 *   <li>静态工厂：提供静态方法访问国际化消息</li>
 *   <li>延迟加载：支持消息的延迟加载</li>
 * </ul>
 *
 * <p>使用方法：
 * <pre>
 * String message = JavaDocBundle.message("notification.title");
 * String formatted = JavaDocBundle.message("notification.completion.format", completed, failed, skipped);
 * Supplier<String> messageSupplier = JavaDocBundle.messagePointer("action.generate.javadoc");
 * </pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @see DynamicBundle
 * @since 1.0.0
 */
public class JavaDocBundle extends DynamicBundle {

    /** 消息资源包名称，用于加载国际化消息 */
    @NonNls
    private static final String BUNDLE = "messages.AIJBundle";

    /**
     * 单例实例
     *
     * <p>JavaDocBundle 的全局唯一实例。
     * 采用饿汉式单例模式，线程安全。
     *
     * @see #message(String, Object...)
     * @see #messagePointer(String, Object...)
     */
    private static final JavaDocBundle INSTANCE = new JavaDocBundle();

    /**
     * 私有构造函数
     *
     * <p>初始化 DynamicBundle，加载指定的资源包。
     * 防止外部直接实例化，确保单例模式。
     *
     * @see DynamicBundle#DynamicBundle(String)
     */
    private JavaDocBundle() {
        super(BUNDLE);
    }

    /**
     * 获取国际化消息
     *
     * <p>根据资源键获取对应语言环境的国际化消息。
     * 支持参数格式化，可传递参数替换消息中的占位符。
     *
     * <p>使用场景：
     * <ul>
     *   <li>获取简单的文本消息</li>
     *   <li>获取带参数的格式化消息</li>
     *   <li>在需要立即显示消息的地方使用</li>
     * </ul>
     *
     * <p>示例：
     * <pre>
     * String title = JavaDocBundle.message("notification.title");
     * String format = JavaDocBundle.message("notification.completion.format", 5, 0, 2);
     * </pre>
     *
     * @param key    资源键，对应 messages.properties 中的键
     * @param params 格式化参数，用于替换消息中的 {0}, {1} 等占位符
     * @return 国际化后的消息字符串
     * @see DynamicBundle#getMessage(String, Object...)
     */
    @NotNull
    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    /**
     * 获取国际化消息（延迟加载）
     *
     * <p>返回消息的 Supplier，支持延迟加载。
     * 在需要延迟获取消息或传递给支持 Supplier 的 API 时使用。
     *
     * <p>使用场景：
     * <ul>
     *   <li>需要延迟加载消息的场景</li>
     *   <li>传递给接受 Supplier 参数的 API</li>
     *   <li>避免在初始化时加载所有消息</li>
     * </ul>
     *
     * <p>示例：
     * <pre>
     * Supplier<String> titleSupplier = JavaDocBundle.messagePointer("action.generate.javadoc");
     * AnAction action = new AnAction(titleSupplier, descriptionSupplier, icon);
     * </pre>
     *
     * @param key    资源键，对应 messages.properties 中的键
     * @param params 格式化参数，用于替换消息中的 {0}, {1} 等占位符
     * @return 消息提供者，调用 get() 方法获取实际消息
     * @see DynamicBundle#getLazyMessage(String, Object...)
     */
    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                                  Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}


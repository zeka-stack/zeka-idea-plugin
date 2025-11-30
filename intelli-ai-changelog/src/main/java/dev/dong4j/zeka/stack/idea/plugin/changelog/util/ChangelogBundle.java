package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import com.intellij.DynamicBundle;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * 变更日志资源包类
 * <p>
 * 继承自 DynamicBundle, 用于管理变更日志相关的国际化消息资源.
 * 提供静态方法来获取本地化的消息字符串, 支持参数替换和延迟加载功能.
 * 该类采用单例模式, 确保资源包的统一管理和高效访问.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class ChangelogBundle extends DynamicBundle {

    /** 消息资源包名称，用于加载国际化消息 */
    @NonNls
    private static final String BUNDLE = "messages.ChangelogBundle";

    /**
     * 单例实例
     *
     * <p>ChangelogBundle 的全局唯一实例。
     * 采用饿汉式单例模式，线程安全。
     *
     * @see #message(String, Object...)
     * @see #messagePointer(String, Object...)
     */
    private static final ChangelogBundle INSTANCE = new ChangelogBundle();

    /**
     * 私有构造函数
     *
     * <p>初始化 DynamicBundle，加载指定的资源包。
     * 防止外部直接实例化，确保单例模式。
     *
     * @see DynamicBundle#DynamicBundle(String)
     */
    private ChangelogBundle() {
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
     * String title = ChangelogBundle.message("notification.title");
     * String format = ChangelogBundle.message("notification.completion.format", 5, 0, 2);
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
     * Supplier<String> titleSupplier = ChangelogBundle.messagePointer("action.generate.changelog");
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

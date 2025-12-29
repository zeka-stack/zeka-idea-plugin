package dev.dong4j.zeka.stack.idea.plugin.example.util;

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
 * @author dong4j
 * @since 1.0.0
 */
public class ExampleBundle extends DynamicBundle {

    @NonNls
    private static final String BUNDLE = "messages.ExampleBundle";

    private static final ExampleBundle INSTANCE = new ExampleBundle();

    private ExampleBundle() {
        super(BUNDLE);
    }

    @NotNull
    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                                  Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}


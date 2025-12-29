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
 * @author dong4j
 * @version 1.0.0
 */
public class HelperBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "messages.HelperBundle";
    private static final HelperBundle INSTANCE = new HelperBundle();

    private HelperBundle() {
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


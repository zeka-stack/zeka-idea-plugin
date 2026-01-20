package dev.dong4j.zeka.stack.idea.plugin.repairer.util;

import com.intellij.DynamicBundle;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * Repairer 资源包类.
 */
public class RepairerBundle extends DynamicBundle {

    @NonNls
    private static final String BUNDLE = "messages.RepairerBundle";

    private static final RepairerBundle INSTANCE = new RepairerBundle();

    private RepairerBundle() {
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

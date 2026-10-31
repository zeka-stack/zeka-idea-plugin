package dev.dong4j.zeka.stack.idea.plugin.common.ai.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * 远程推荐模型目录条目
 *
 * @param defaultModel 推荐默认模型，可空
 * @param models       推荐模型列表
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public record ModelCatalogEntry(
    @Nullable String defaultModel,
    @NotNull List<String> models
) {
    public static final ModelCatalogEntry EMPTY = new ModelCatalogEntry(null, List.of());

    /**
     * 规范化：过滤空白模型名，列表不可变
     */
    @NotNull
    public static ModelCatalogEntry of(@Nullable String defaultModel, @Nullable List<String> models) {
        if (models == null || models.isEmpty()) {
            return new ModelCatalogEntry(blankToNull(defaultModel), List.of());
        }
        List<String> cleaned = models.stream()
            .filter(m -> m != null && !m.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
        String def = blankToNull(defaultModel);
        if (def == null && !cleaned.isEmpty()) {
            def = cleaned.getFirst();
        }
        return new ModelCatalogEntry(def, Collections.unmodifiableList(cleaned));
    }

    public boolean isEmpty() {
        return models.isEmpty();
    }

    @Nullable
    private static String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

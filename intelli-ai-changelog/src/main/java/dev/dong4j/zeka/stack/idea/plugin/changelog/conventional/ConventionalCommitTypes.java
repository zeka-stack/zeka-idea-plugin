package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;

/**
 * 与 AI 提示词对齐的标准 Conventional Commit {@code type} 白名单。
 * <p>
 * 供首行 type 补全与校验使用；补全列表右侧展示 {@link #description(String)} 说明文案。
 *
 * @author dong4j
 * @since 1.0.0
 */
public final class ConventionalCommitTypes {

    public static final List<String> ALL = List.of(
        "feat", "fix", "refactor", "perf", "docs",
        "test", "build", "chore", "style", "revert"
    );

    private static final Map<String, String> DESCRIPTION_KEYS = Map.of(
        "feat", "commit.conventional.type.feat",
        "fix", "commit.conventional.type.fix",
        "refactor", "commit.conventional.type.refactor",
        "perf", "commit.conventional.type.perf",
        "docs", "commit.conventional.type.docs",
        "test", "commit.conventional.type.test",
        "build", "commit.conventional.type.build",
        "chore", "commit.conventional.type.chore",
        "style", "commit.conventional.type.style",
        "revert", "commit.conventional.type.revert"
    );

    private ConventionalCommitTypes() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isStandard(@NotNull String type) {
        return ALL.contains(type);
    }

    @NotNull
    public static List<String> matchesPrefix(@NotNull String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return ALL.stream().filter(t -> t.startsWith(p)).toList();
    }

    /**
     * 返回 type 的用户可见说明（已国际化），未知 type 返回空串。
     */
    @NotNull
    public static String description(@NotNull String type) {
        String key = DESCRIPTION_KEYS.get(type);
        if (key == null) {
            return "";
        }
        return ChangelogBundle.message(key);
    }
}

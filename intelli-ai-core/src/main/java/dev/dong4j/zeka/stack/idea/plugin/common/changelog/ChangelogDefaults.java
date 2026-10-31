package dev.dong4j.zeka.stack.idea.plugin.common.changelog;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Changelog 默认模板与配置入口
 * <p>
 * 集中提供 changelog 生成的默认 prompt 模板与 git-cliff 配置, 资源文件位于
 * {@code classpath:prompts/}, 由插件宿主(SettingsState 默认值)与 MCP Server 宿主共用,
 * 保证两个宿主的默认生成行为一致, 避免模板两处维护导致漂移.
 * <p>
 * 模板中的占位符保持与插件版一致:
 * <ul>
 *   <li>{@code ${language}}: 响应语言(由调用方按 ResponseLanguage 替换)</li>
 *   <li>{@code {version}} / {@code {commits}} / {@code {date}} / {@code {dateRange}} / {@code {codeDiffs}}: 上下文占位符</li>
 * </ul>
 * 资源在首次读取后缓存, 后续调用零 I/O.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ChangelogDefaults {

    /** 资源路径 -> 已缓存内容 */
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    /**
     * 私有构造函数, 防止实例化工具类
     */
    private ChangelogDefaults() {
    }

    /**
     * 获取默认的 changelog 系统提示词
     *
     * @return 默认系统提示词
     */
    public static @NotNull String getDefaultChangelogSystemPrompt() {
        return load("prompts/changelog-system-prompt.txt");
    }

    /**
     * 获取默认的 changelog 用户提示词模板
     *
     * @return 默认 changelog 模板
     */
    public static @NotNull String getDefaultChangelogUserPrompt() {
        return load("prompts/changelog-user-prompt.txt");
    }

    /**
     * 获取默认的日报模板
     *
     * @return 默认日报模板
     */
    public static @NotNull String getDefaultDailyReportUserPrompt() {
        return load("prompts/daily-report-user-prompt.txt");
    }

    /**
     * 获取默认的周报模板
     *
     * @return 默认周报模板
     */
    public static @NotNull String getDefaultWeeklyReportUserPrompt() {
        return load("prompts/weekly-report-user-prompt.txt");
    }

    /**
     * 获取默认的 commit message 用户提示词模板
     *
     * @return 默认 commit message 模板
     */
    public static @NotNull String getDefaultCommitMessageUserPrompt() {
        return load("prompts/commit-message-user-prompt.txt");
    }

    /**
     * 获取默认的 commit message 系统提示词
     *
     * @return 默认 commit message 系统提示词
     */
    public static @NotNull String getDefaultCommitMessageSystemPrompt() {
        return load("prompts/commit-message-system-prompt.txt");
    }

    /**
     * 获取默认的 git-cliff 配置(TOML)
     *
     * @return 默认 git-cliff 配置文本
     */
    public static @NotNull String getDefaultGitCliffConfig() {
        return load("prompts/git-cliff-config.toml");
    }

    /**
     * 获取默认的 release log 用户提示词模板
     *
     * @return 默认 release log 模板
     */
    public static @NotNull String getDefaultReleaseLogUserPrompt() {
        return load("prompts/release-log-user-prompt.txt");
    }

    /**
     * 获取默认的生成排除模式列表
     * <p>
     * 命中的文件(diff 路径)不进入 changelog 上下文, 与插件版默认值保持一致.
     *
     * @return 可变的排除模式列表
     */
    public static @NotNull List<String> getDefaultExcludePatterns() {
        return new ArrayList<>(Arrays.asList(
            "*.pb.go",
            "*.pb.cc",
            "*.pb.h",
            "go.sum",
            "go.mod",
            "package-lock.json",
            "yarn.lock",
            "pnpm-lock.yaml",
            "Cargo.lock",
            "Pipfile.lock",
            "poetry.lock",
            "*.generated.*",
            "*.gen.*",
            "*_generated.*",
            "*_gen.*",
            "vendor/**",
            "node_modules/**",
            ".next/**",
            "dist/**",
            "build/**",
            "target/**",
            "*.min.js",
            "*.min.css",
            "*.bundle.*",
            "*.chunk.*",
            "coverage/**",
            ".nyc_output/**",
            "*.lcov",
            "*.log",
            "*.tmp",
            "*.temp",
            ".DS_Store",
            "Thumbs.db",
            "*.swp",
            "*.swo",
            "*~"
        ));
    }

    /**
     * 读取 classpath 资源(带缓存)
     *
     * @param path 资源路径
     * @return 资源文本内容
     * @throws IllegalStateException 资源缺失时抛出(属于构建期错误, 不应静默降级为空模板)
     */
    private static @NotNull String load(@NotNull String path) {
        return CACHE.computeIfAbsent(path, p -> {
            try (InputStream is = ChangelogDefaults.class.getClassLoader().getResourceAsStream(p)) {
                if (is == null) {
                    throw new IllegalStateException("缺少 core 模板资源: " + p);
                }
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[8192];
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, n);
                    }
                    return sb.toString();
                }
            } catch (IOException e) {
                throw new IllegalStateException("读取 core 模板资源失败: " + p, e);
            }
        });
    }
}

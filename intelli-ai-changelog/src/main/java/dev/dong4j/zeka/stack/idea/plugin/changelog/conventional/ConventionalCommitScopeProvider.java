package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogGitService;

/**
 * 项目级 Conventional Commit {@code scope} 轻量缓存服务。
 * <p>
 * 通过读取当前项目最近的若干条 Git 提交消息，解析出其中曾使用过的 {@code scope} 片段，
 * 供 {@link ConventionalCommitCompletionContributor} 在 {@link ConventionalCommitContext#SCOPE}
 * 语义片段内提供“最近用过的 scope”补全建议。
 * <p>
 * 设计要点：
 * <ul>
 *     <li>Git 历史读取（{@link ChangelogGitService#buildRecentCommitMessagesText(int)}）在后台线程
 *         （{@link ApplicationManager#executeOnPooledThread}）执行，不阻塞 EDT；补全逻辑只读取内存缓存
 *         {@link #getRecentScopes()}，即使缓存为空也不等待刷新完成；</li>
 *     <li>缓存 TTL 约 {@value #CACHE_TTL_MILLIS} 毫秒：过期后 {@link #getRecentScopes()} 会触发一次
 *         异步刷新，但仍立即返回刷新前的旧缓存；</li>
 *     <li>缓存为空（如项目非 Git 仓库、暂无历史提交、历史中从未写过 scope）时不返回任何“伪造”的 scope。</li>
 * </ul>
 * <p>
 * 通过 {@code projectService} 扩展点注册为项目级单例，业务代码应使用 {@link #getInstance(Project)} 获取实例，
 * 不应直接调用构造函数。
 *
 * @author dong4j
 * @since 1.0.0
 */
public final class ConventionalCommitScopeProvider {

    /** 缓存过期时间（毫秒），约 60 秒。 */
    private static final long CACHE_TTL_MILLIS = 60_000L;

    /** 读取最近提交消息的数量上限，用于限制单次 Git 历史读取的规模。 */
    private static final int RECENT_COMMITS_LIMIT = 30;

    /** 当前项目实例。 */
    private final Project project;

    /** 最近一次刷新得到的 scope 列表（按出现频次从高到低排序、已去重），初始为空列表。 */
    private volatile List<String> cachedScopes = List.of();

    /** 最近一次刷新完成的时间戳（{@link System#currentTimeMillis()}），初始为 0 表示从未刷新成功过。 */
    private volatile long cachedAtMillis;

    /** 避免同一时刻并发触发多次刷新任务，读取 Git 历史开销不小，不需要重复执行。 */
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    /**
     * 构造函数。
     * <p>
     * 由 IntelliJ 平台通过 {@code projectService} 扩展点创建，业务代码应使用 {@link #getInstance(Project)} 获取实例。
     *
     * @param project 项目实例，不能为 null
     */
    public ConventionalCommitScopeProvider(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 获取当前项目对应的 {@link ConventionalCommitScopeProvider} 单例实例。
     *
     * @param project 项目实例，不能为 null
     * @return 项目级单例实例
     */
    @NotNull
    public static ConventionalCommitScopeProvider getInstance(@NotNull Project project) {
        return project.getService(ConventionalCommitScopeProvider.class);
    }

    /**
     * 获取最近使用过的 scope 列表（按出现频次从高到低排序，已去重）。
     * <p>
     * 若缓存已超过 {@value #CACHE_TTL_MILLIS} 毫秒未刷新，会触发一次 {@link #refreshAsync()}，
     * 但本次调用仍立即返回刷新前的缓存内容（可能为空列表），不会阻塞调用方（如补全逻辑）。
     *
     * @return 最近使用过的 scope 列表；从未成功刷新过或历史提交中没有任何 scope 时返回空列表
     */
    @NotNull
    public List<String> getRecentScopes() {
        if (System.currentTimeMillis() - cachedAtMillis > CACHE_TTL_MILLIS) {
            refreshAsync();
        }
        return cachedScopes;
    }

    /**
     * 异步刷新 scope 缓存。
     * <p>
     * 在后台线程读取最近的 Git 提交消息（{@link ChangelogGitService#buildRecentCommitMessagesText(int)}）
     * 并解析出其中的 scope（{@link #extractScopes(String)}），避免阻塞 EDT。同一时刻只允许一个刷新任务在执行，
     * 重复调用会被直接忽略。
     */
    public void refreshAsync() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ChangelogGitService gitService = new ChangelogGitService(project);
                String recentMessagesText = gitService.buildRecentCommitMessagesText(RECENT_COMMITS_LIMIT);
                cachedScopes = extractScopes(recentMessagesText);
            } finally {
                cachedAtMillis = System.currentTimeMillis();
                refreshing.set(false);
            }
        });
    }

    /**
     * 从最近提交消息文本中提取 scope 列表。
     * <p>
     * 文本每行形如 {@code "- feat(core): xxx"}（见 {@link ChangelogGitService#buildRecentCommitMessagesText(int)}）；
     * 逐行剥离前缀 {@code "- "} 后交由 {@link ConventionalCommitHeaderParser#parseFirstLine} 解析出 scope，
     * 空 scope 直接忽略。最终结果按出现频次从高到低排序（频次相同时保留首次出现顺序，即“频次排序去重”）。
     *
     * @param recentMessagesText 最近提交消息文本，可能为空字符串
     * @return 去重后的 scope 列表；历史消息中不包含任何 scope 时返回空列表
     */
    @NotNull
    private static List<String> extractScopes(@NotNull String recentMessagesText) {
        if (recentMessagesText.isBlank()) {
            return List.of();
        }

        // LinkedHashMap 保留首次出现顺序，供频次相同时的排序 tie-break 使用
        Map<String, Integer> frequencyByScope = new LinkedHashMap<>();
        for (String line : recentMessagesText.split("\\R")) {
            String message = stripLeadingListMarker(line);
            if (message.isBlank()) {
                continue;
            }
            ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine(message);
            String scope = header.scope();
            if (scope == null || scope.isBlank()) {
                continue;
            }
            frequencyByScope.merge(scope.trim(), 1, Integer::sum);
        }

        if (frequencyByScope.isEmpty()) {
            return List.of();
        }

        List<String> scopesByFrequency = new ArrayList<>(frequencyByScope.keySet());
        // List.sort 使用稳定排序（TimSort），频次相同的 scope 保持 LinkedHashMap 中的首次出现顺序
        scopesByFrequency.sort((left, right) -> frequencyByScope.get(right) - frequencyByScope.get(left));
        return List.copyOf(scopesByFrequency);
    }

    /**
     * 剥离 {@link ChangelogGitService#buildRecentCommitMessagesText(int)} 输出每行前缀的 {@code "- "} 列表标记。
     *
     * @param line 原始行文本
     * @return 去除列表标记后的提交消息文本
     */
    @NotNull
    private static String stripLeadingListMarker(@NotNull String line) {
        String trimmed = line.stripLeading();
        return trimmed.startsWith("- ") ? trimmed.substring(2) : trimmed;
    }
}

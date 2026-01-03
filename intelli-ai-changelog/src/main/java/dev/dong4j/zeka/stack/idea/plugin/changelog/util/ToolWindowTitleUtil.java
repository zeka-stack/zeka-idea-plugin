package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具窗口标题工具类
 * <p>
 * 提供统一的工具窗口标题生成功能，将 action 的国际化 key 映射为简称，并组合时间戳生成标题。
 * 格式为：简称:时间戳（例如：RL:20260103114605）
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ToolWindowTitleUtil {

    /**
     * 时间格式化器，格式为 "yyyyMMddHHmmss"
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Action 国际化 key 到简称的映射
     * <p>
     * 映射关系：
     * - action.generate.changelog.diff → CD
     * - action.generate.changelog.gitlog → CM
     * - action.generate.daily.report.gitlog → DR
     * - action.generate.weekly.report.gitlog → WR
     * - action.generate.release.log → RL
     */
    private static final Map<String, String> ACTION_KEY_TO_ABBREVIATION = new HashMap<>();

    static {
        ACTION_KEY_TO_ABBREVIATION.put("action.generate.changelog.diff", "CD");
        ACTION_KEY_TO_ABBREVIATION.put("action.generate.changelog.gitlog", "CM");
        ACTION_KEY_TO_ABBREVIATION.put("action.generate.daily.report.gitlog", "DR");
        ACTION_KEY_TO_ABBREVIATION.put("action.generate.weekly.report.gitlog", "WR");
        ACTION_KEY_TO_ABBREVIATION.put("action.generate.release.log", "RL");
    }

    /**
     * 私有构造函数，防止实例化
     */
    private ToolWindowTitleUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 构建工具窗口标题
     * <p>
     * 根据 action 的国际化 key 获取简称，并组合当前时间戳生成标题。
     * 格式为：简称:时间戳（例如：RL:20260103114605）
     * <p>
     * 如果 actionKey 不在映射表中，则使用 actionKey 的最后一个单词作为简称。
     *
     * @param actionKey Action 的国际化 key（例如："action.generate.release.log"）
     * @return 格式化的标题字符串（例如："RL:20260103114605"）
     */
    @NotNull
    public static String buildToolWindowTitle(@NotNull String actionKey) {
        String abbreviation = ACTION_KEY_TO_ABBREVIATION.getOrDefault(actionKey, getDefaultAbbreviation(actionKey));
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        return abbreviation + ":" + timestamp;
    }

    /**
     * 从工具窗口标题中提取分类标识
     * <p>
     * 标题格式为：简称:时间戳（例如：RL:20260103114605）
     * 分类标识为冒号前的简称部分
     *
     * @param title 工具窗口标题
     * @return 分类标识
     */
    @NotNull
    public static String extractCategory(@NotNull String title) {
        int index = title.indexOf(':');
        if (index > 0) {
            return title.substring(0, index);
        }
        return title;
    }

    /**
     * 获取默认简称
     * <p>
     * 当 actionKey 不在映射表中时，使用 actionKey 的最后一个单词作为简称。
     * 例如："action.generate.unknown.action" → "action"
     *
     * @param actionKey Action 的国际化 key
     * @return 默认简称
     */
    @NotNull
    private static String getDefaultAbbreviation(@NotNull String actionKey) {
        int lastDotIndex = actionKey.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < actionKey.length() - 1) {
            String lastWord = actionKey.substring(lastDotIndex + 1);
            // 取前两个字符并转为大写
            return lastWord.length() >= 2
                   ? lastWord.substring(0, 2).toUpperCase()
                   : lastWord.toUpperCase();
        }
        // 如果没有点，使用整个字符串的前两个字符
        return actionKey.length() >= 2
               ? actionKey.substring(0, 2).toUpperCase()
               : actionKey.toUpperCase();
    }
}

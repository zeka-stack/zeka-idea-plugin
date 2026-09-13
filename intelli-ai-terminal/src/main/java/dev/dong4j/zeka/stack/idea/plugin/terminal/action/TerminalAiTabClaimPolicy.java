package dev.dong4j.zeka.stack.idea.plugin.terminal.action;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 终端 AI 对 Tab 键的抢占策略
 * <p>
 * 插件在 keymap 中绑定了 {@code TAB}, 若始终参与快捷键分发, 会覆盖编辑器缩进 / Live Template,
 * 以及 Reworked Terminal 的命令补全与 shell Tab 补全.
 * 因此只有在「终端 AI 已启用 + 焦点在终端 + 当前行已提取到有效 AI 输入」时才占用 Tab,
 * 其余场景必须让出, 交给 IDE / 终端默认处理.
 * </p>
 *
 * @author dong4j
 * @version 2026.2.1010
 * @since 2026.2.1010
 */
public final class TerminalAiTabClaimPolicy {

    private TerminalAiTabClaimPolicy() {
        // 策略工具类, 禁止实例化
    }

    /**
     * 判断当前是否应由终端 AI 占用 Tab
     * <p>
     * 该方法必须保持轻量: {@code ActionPromoter} 在无 Read Lock 下调用, 不能做 PSI / 索引解析.
     * </p>
     *
     * @param enableTerminalAI 是否启用终端 AI
     * @param inTerminal       当前焦点是否在终端
     * @param extractedInput   已按触发前缀提取的 AI 输入, 无有效输入时为 null
     * @return true 表示占用 Tab 并触发 AI 生成; false 表示让出默认 Tab
     */
    public static boolean shouldClaimTab(boolean enableTerminalAI,
                                         boolean inTerminal,
                                         @Nullable String extractedInput) {
        return enableTerminalAI && inTerminal && hasContent(extractedInput);
    }

    /**
     * 从已规范化的逻辑行中提取触发前缀后的内容
     * <p>
     * 与 {@link TerminalAiGenerateAction} 的触发行解析保持同一规则:
     * <ul>
     *     <li>未配置前缀时, 整行都视为 AI 输入</li>
     *     <li>行内找不到前缀, 或前缀后没有内容, 返回 null (此时必须让出 Tab)</li>
     * </ul>
     * </p>
     *
     * @param triggerPrefix 触发前缀, 允许为 null 或空白
     * @param logicalLine   已去掉 prompt 后的逻辑行
     * @return 前缀后的有效内容; 不应占用 Tab 时返回 null
     */
    @Nullable
    public static String extractTriggerContent(@Nullable String triggerPrefix, @Nullable String logicalLine) {
        if (logicalLine == null || logicalLine.isBlank()) {
            return null;
        }
        String prefix = triggerPrefix == null ? "" : triggerPrefix.trim();
        String trimmed = logicalLine.stripLeading();
        if (prefix.isEmpty()) {
            String wholeLine = trimmed.strip();
            return wholeLine.isEmpty() ? null : wholeLine;
        }
        int index = trimmed.indexOf(prefix);
        if (index < 0) {
            return null;
        }
        String content = trimmed.substring(index + prefix.length());
        if (content.startsWith(" ")) {
            content = content.substring(1);
        }
        String stripped = content.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private static boolean hasContent(@Nullable String extractedInput) {
        return extractedInput != null && !extractedInput.isBlank();
    }
}

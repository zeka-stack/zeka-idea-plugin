package dev.dong4j.zeka.stack.idea.plugin.terminal.terminal;

import com.intellij.terminal.frontend.view.TerminalAllowedActionsProvider;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.terminal.action.TerminalAiGenerateAction;

/**
 * 终端 AI 允许操作提供者
 * <p> 该类实现了 TerminalAllowedActionsProvider 接口, 主要负责提供终端 AI 插件中允许执行的动作 ID 列表
 * <p> 当前实现返回包含终端 AI 生成操作的唯一动作标识符, 用于权限控制和安全检查
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
@SuppressWarnings("UnstableApiUsage")
public class TerminalAiAllowedActionsProvider implements TerminalAllowedActionsProvider {
    /**
     * 获取允许在终端中触发的动作 ID 列表
     * <p> 返回一个包含单个动作 ID 的不可变列表, 该动作用于在终端中触发 AI 生成功能
     *
     * @return 包含唯一动作 ID 的不可变列表, 值为 <pre>{@code "dev.dong4j.zeka.stack.idea.plugin.terminal.action.TerminalAiGenerateAction"}</pre>
     * @see TerminalAiGenerateAction
     */
    @Override
    public @NotNull List<String> getActionIds() {
        return List.of("dev.dong4j.zeka.stack.idea.plugin.terminal.action.TerminalAiGenerateAction");
    }
}

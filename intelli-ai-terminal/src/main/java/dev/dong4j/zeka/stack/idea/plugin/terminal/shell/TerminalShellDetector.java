package dev.dong4j.zeka.stack.idea.plugin.terminal.shell;

import com.intellij.terminal.JBTerminalWidget;
import com.intellij.terminal.frontend.view.TerminalView;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Terminal Shell 类型检测器
 * <p>根据操作系统信息推断当前终端所处平台, Windows 下使用独立分支逻辑.</p>
 *
 * @since 1.0.0
 */
public final class TerminalShellDetector {

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该类为工具类, 仅提供静态方法, 不允许创建实例 </p>
     */
    private TerminalShellDetector() {
    }

    /**
     * 检测当前执行环境使用的 Shell 类型
     *
     * @param terminalView 终端视图, 可以为空
     * @param widget       终端控件, 可以为空
     * @return 推断的 Shell 类型
     */
    public static TerminalShellType detect(@Nullable TerminalView terminalView,
                                           @Nullable JBTerminalWidget widget) {
        String osName = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return TerminalShellType.WINDOWS;
        }
        return TerminalShellType.UNIX;
    }
}

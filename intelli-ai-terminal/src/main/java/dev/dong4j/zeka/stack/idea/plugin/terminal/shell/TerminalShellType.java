package dev.dong4j.zeka.stack.idea.plugin.terminal.shell;

/**
 * Terminal Shell 类型
 * <p>用于在终端输入/输出处理过程中对不同操作系统或 Shell 类型进行分支,
 * 当前仅区分 Unix 风格与 Windows 风格的行为.</p>
 *
 * @since 1.0.0
 */
public enum TerminalShellType {

    /**
     * 类 Unix Shell（bash/zsh/fish/Git Bash/WSL 等）
     */
    UNIX,
    /**
     * Windows Shell（PowerShell / cmd）
     */
    WINDOWS
}

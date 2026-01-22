# Terminal AI Windows 支持方案

## 目标

在 Windows 环境（PowerShell / cmd / Git Bash / WSL）下，使 Terminal AI 的输入解析、生成命令、回写逻辑可用且符合 Windows 习惯。

## 需要支持的终端类型

1. **PowerShell（优先）**
2. **cmd（兼容）**
3. **Git Bash（类 Unix，可复用现有逻辑）**
4. **WSL（类 Unix，可复用现有逻辑）**

## 关键改造点

### 1. Prompt 模板分流

- 当前 System Prompt 明确要求 bash/zsh，Windows 会误导生成命令。
- 需要按 Shell 类型切换 System Prompt / User Prompt。
- 建议增加：`systemPromptWindows`、`terminalTemplateWindows`。

### 2. 输入行清理与回写

- 当前使用 `Ctrl+U` / `Ctrl+C` 清行，PowerShell/cmd 不一定生效。
- 需要为 Windows 终端定义新的“清行 + 回写”策略：
    - PowerShell：尝试 `Ctrl+U` / `Ctrl+C` + `Clear-Host` / `Esc` 组合
    - cmd：可能需要发送 `Esc` 或 `Ctrl+U` 兼容方案
- 建议根据 Shell 类型选择替换策略。

### 3. 触发前缀与提示符解析

- Windows prompt 样式不同：`PS C:\path>` / `C:\path>`
- 需要增强 `stripPromptPrefix()` 支持：
    - PowerShell 前缀：`PS ` + `>` 结构
    - cmd 前缀：`C:\...>` 结构
- 保持 Unix 前缀兼容。

### 4. 输出校验规则

- 当前校验正则偏 Unix（`^[a-zA-Z0-9/.$]`）
- Windows 命令常以字母开头，但也可能以 `.`、`&`、`.\`、`C:` 开头
- 建议按 Shell 分支校验逻辑：
    - PowerShell：允许 `Get-`、`Set-`、`.\`、`&`、`C:` 等开头
    - cmd：允许 `dir`、`copy`、`type`、`C:` 等开头

### 5. 上下文检测与路径处理

- Windows 路径分隔符为 `\`，需要处理转义和提示词展示。
- `currentDirectory` 可能包含盘符（如 `C:\`）。
- 输出命令应避免 Unix-only 工具（如 `grep`、`find`）。

### 6. 状态提示与 ANSI 颜色

- Windows 终端对 ANSI 支持不一致。
- 建议在 Windows 模式下禁用 ANSI 颜色，使用纯文本提示。

### 7. 终端能力识别

- 需要识别当前 Shell：
    - 环境变量 `SHELL` 在 Windows 不可靠
    - 可根据 prompt 或终端类型推断（PowerShell/cmd/WSL）
- 建议新增 `TerminalShellDetector`：
    - 读取系统信息 + 终端上下文 + prompt 结构综合判断

## 最小可用实现（建议优先级）

1. **支持 PowerShell**：
    - 独立 Prompt 模板
    - 清行策略调整
    - 解析 `PS C:\...>` 提示符
2. **支持 cmd**：
    - 仅提供基础命令生成
3. **Git Bash / WSL**：
    - 复用现有 Unix 逻辑

## 兼容性风险

- Windows 终端对控制字符支持差异大
- Prompt 结构可能被用户自定义
- ANSI 颜色不稳定，需要降级策略

## 结论

Windows 支持不是“新增 API”，而是“行为分支 + 兼容策略”的工作。
建议先落地 PowerShell 最小版本，再逐步覆盖 cmd 与更多终端。

## 实现说明（当前分支逻辑）

1. 新增 `TerminalShellType` + `TerminalShellDetector`，通过 `os.name` 判断是否进入 Windows 分支，保持 Unix/WSL/Git Bash 走原来的逻辑；AI
   生成核心流程复用原实现，只需携带 shell type。
2. `TerminalAiGenerateAction` 在输入提取时传入 shell type：Windows 分支会解析 `PS C:\...>` / `C:\...>` 样式的提示符，Unix 继续沿用 `$`/`>` 规则。
3. 替换命令行时新增 Windows 分支：先发送 `Ctrl+C` + `Ctrl+U`，再写入命令；`isValidShellOutput` 也支持 `C:`、`./`、`&` 等开头，确保 PowerShell/cmd
   结果不被误判。
4. Windows 分支仍复用 AI 调用、上下文收集、控制台日志等逻辑，真正差异只集中在 prompt 清理、行替换和输出校验，满足“尽量少动现有逻辑”的目标。

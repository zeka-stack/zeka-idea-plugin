/**
 * Global window interface extensions for IDEA plugin communication
 */
interface Window {
  /**
   * Send message to Java backend
   */
  sendToJava?: (message: string) => void;

  /**
   * Get clipboard file path from Java
   */
  getClipboardFilePath?: () => Promise<string>;

  /**
   * Handle file path dropped from Java
   */
  handleFilePathFromJava?: (filePath: string) => void;

  /**
   * Update messages from backend
   */
  updateMessages?: (json: string) => void;

  /**
   * Update status message
   */
  updateStatus?: (text: string) => void;

  /**
   * Update engine providers list (IntelliAI Engine)
   */
  updateEngineProviders?: (json: string) => void;

  /**
   * Show loading indicator
   */
  showLoading?: (value: string | boolean) => void;

  /**
   * Show thinking status
   */
  showThinkingStatus?: (value: string | boolean) => void;

  /**
   * Set history data
   */
  setHistoryData?: (data: any) => void;

  /**
   * Export session data callback
   */
  onExportSessionData?: (json: string) => void;

  /**
   * Clear all messages
   */
  clearMessages?: () => void;

  /**
   * Add error message
   */
  addErrorMessage?: (message: string) => void;

  /**
   * Add single history message (used for Codex session loading)
   */
  addHistoryMessage?: (message: any) => void;

  /**
   * Add user message to chat (used for external Quick Fix feature)
   * Immediately shows the user's message in the chat UI before AI response
   */
  addUserMessage?: (content: string) => void;

  /**
   * Set current session ID (for rewind feature)
   */
  setSessionId?: (sessionId: string) => void;

  /**
   * Add toast notification (called from backend)
   */
  addToast?: (message: string, type: 'success' | 'error' | 'warning' | 'info') => void;

  /**
   * Usage statistics update callback
   */
  onUsageUpdate?: (json: string) => void;

  /**
   * Mode changed callback
   */
  onModeChanged?: (mode: string) => void;

  /**
   * Mode received callback - 后端主动推送权限模式（窗口初始化时调用）
   */
  onModeReceived?: (mode: string) => void;

  /**
   * Model changed callback
   */
  onModelChanged?: (modelId: string) => void;

  /**
   * Model confirmed callback - 后端确认模型设置成功后调用
   * @param modelId 确认的模型 ID
   * @param provider 当前的提供商
   */
  onModelConfirmed?: (modelId: string, provider: string) => void;

  /**
   * Show permission dialog
   */
  showPermissionDialog?: (json: string) => void;

  /**
   * Show AskUserQuestion dialog
   */
  showAskUserQuestionDialog?: (json: string) => void;

  /**
   * Show PlanApproval dialog
   */
  showPlanApprovalDialog?: (json: string) => void;

  /**
   * Add selection info (file and line numbers) - 自动监听，只更新 ContextBar
   */
  addSelectionInfo?: (selectionInfo: string) => void;

  /**
   * Add code snippet to input box - 手动发送，添加代码片段标签到输入框
   */
  addCodeSnippet?: (selectionInfo: string) => void;

  /**
   * Insert code snippet at cursor position - 由 ChatInputBox 注册
   */
  insertCodeSnippetAtCursor?: (selectionInfo: string) => void;

  /**
   * Clear selection info
   */
  clearSelectionInfo?: () => void;

  /**
   * File list result callback (for file reference provider)
   */
  onFileListResult?: (json: string) => void;

  /**
   * Command list result callback (for slash command provider)
   */
  onCommandListResult?: (json: string) => void;

  /**
   * Update MCP servers list
   */
  updateMcpServers?: (json: string) => void;

  /**
   * Update MCP server connection status
   */
  updateMcpServerStatus?: (json: string) => void;

  /**
   * Update MCP server tools list
   */
  updateMcpServerTools?: (json: string) => void;

  mcpServerToggled?: (json: string) => void;

  /**
   * Update Codex MCP servers list (from ~/.codex/config.toml)
   */
  updateCodexMcpServers?: (json: string) => void;

  /**
   * Update Codex MCP server connection status
   */
  updateCodexMcpServerStatus?: (json: string) => void;

  /**
   * Codex MCP server toggled callback
   */
  codexMcpServerToggled?: (json: string) => void;

  /**
   * Codex MCP server added callback
   */
  codexMcpServerAdded?: (json: string) => void;

  /**
   * Codex MCP server updated callback
   */
  codexMcpServerUpdated?: (json: string) => void;

  /**
   * Codex MCP server deleted callback
   */
  codexMcpServerDeleted?: (json: string) => void;

  /**
   * Update providers list
   */
  updateProviders?: (json: string) => void;

  /**
   * Update active provider
   */
  updateActiveProvider?: (providerId: string) => void;

  updateThinkingEnabled?: (json: string) => void;

  /**
   * Update streaming enabled setting
   */
  updateStreamingEnabled?: (json: string) => void;

  /**
   * Update send shortcut setting
   */
  updateSendShortcut?: (json: string) => void;

  /**
   * Update commit AI prompt configuration
   */
  updateCommitPrompt?: (json: string) => void;

  /**
   * Update current Claude config
   */
  updateCurrentClaudeConfig?: (json: string) => void;

  /**
   * Show error message
   */
  showError?: (message: string) => void;

  /**
   * Show switch success message
   */
  showSwitchSuccess?: (message: string) => void;

  /**
   * Update Node.js path
   */
  updateNodePath?: (path: string) => void;

  /**
   * Update working directory configuration
   */
  updateWorkingDirectory?: (json: string) => void;

  /**
   * Show success message
   */
  showSuccess?: (message: string) => void;

  /**
   * Update skills list
   */
  updateSkills?: (json: string) => void;

  /**
   * Skill import result callback
   */
  skillImportResult?: (json: string) => void;

  /**
   * Skill delete result callback
   */
  skillDeleteResult?: (json: string) => void;

  /**
   * Skill toggle result callback
   */
  skillToggleResult?: (json: string) => void;

  /**
   * Update usage statistics
   */
  updateUsageStatistics?: (json: string) => void;

  /**
   * Pending usage statistics before component mounts
   */
  __pendingUsageStatistics?: string;

  /**
   * Update slash commands list (from SDK)
   */
  updateSlashCommands?: (json: string) => void;

  /**
   * Pending slash commands payload before provider initialization
   */
  __pendingSlashCommands?: string;

  /**
   * Pending session ID before App component mounts (for rewind feature)
   */
  __pendingSessionId?: string;

  /**
   * Apply IDEA editor font configuration (called from Java backend)
   * @param config Font configuration object containing fontFamily, fontSize, lineSpacing, fallbackFonts
   */
  applyIdeaFontConfig?: (config: {
    fontFamily: string;
    fontSize: number;
    lineSpacing: number;
    fallbackFonts?: string[];
  }) => void;

  /**
   * Pending font config before applyIdeaFontConfig is registered
   */
  __pendingFontConfig?: {
    fontFamily: string;
    fontSize: number;
    lineSpacing: number;
    fallbackFonts?: string[];
  };

  /**
   * Apply IDEA language configuration (called from Java backend)
   * @param config Language configuration object containing language code and IDEA locale
   */
  applyIdeaLanguageConfig?: (config: {
    language: string;
    ideaLocale?: string;
  }) => void;

  /**
   * Pending language config before applyIdeaLanguageConfig is registered
   */
  __pendingLanguageConfig?: {
    language: string;
    ideaLocale?: string;
  };

  /**
   * Update enhanced prompt result (for prompt enhancer feature)
   */
  updateEnhancedPrompt?: (result: string) => void;

  /**
   * Update session title (called when session title changes)
   */
  updateSessionTitle?: (title: string) => void;

  /**
   * Editor font config received callback - 接收 IDEA 编辑器字体配置
   */
  onEditorFontConfigReceived?: (json: string) => void;

  /**
   * IDE theme received callback - 接收 IDE 主题配置
   */
  onIdeThemeReceived?: (json: string) => void;

  /**
   * IDE theme changed callback - IDE 主题变化时的回调
   */
  onIdeThemeChanged?: (json: string) => void;

  /**
   * Update agents list
   */
  updateAgents?: (json: string) => void;

  /**
   * Agent operation result callback
   */
  agentOperationResult?: (json: string) => void;

  /**
   * Selected agent received callback - 初始化时接收当前选中的智能体
   */
  onSelectedAgentReceived?: (json: string) => void;

  /**
   * Selected agent changed callback - 选择智能体后的回调
   */
  onSelectedAgentChanged?: (json: string) => void;

  /**
   * Update Codex providers list
   */
  updateCodexProviders?: (json: string) => void;

  /**
   * Update active Codex provider
   */
  updateActiveCodexProvider?: (json: string) => void;

  /**
   * Update current Codex config (from ~/.codex/)
   */
  updateCurrentCodexConfig?: (json: string) => void;

// ============================================================================
  // 🔧 流式传输回调函数
  // ============================================================================

  /**
   * Stream start callback - 流式传输开始时调用
   */
  onStreamStart?: () => void;

  /**
   * Content delta callback - 收到内容增量时调用
   * @param delta 内容增量字符串
   */
  onContentDelta?: (delta: string) => void;

  /**
   * Thinking delta callback - 收到思考增量时调用
   * @param delta 思考增量字符串
   */
  onThinkingDelta?: (delta: string) => void;

  /**
   * Stream end callback - 流式传输结束时调用
   */
  onStreamEnd?: () => void;

  /**
   * Permission denied callback - 权限被拒绝时调用
   * 用于标记未完成的工具调用为"中断"状态
   */
  onPermissionDenied?: () => void;

  /**
   * 存储被拒绝的工具调用 ID 集合
   * 用于让工具块知道哪些工具调用被用户拒绝了权限
   */
  __deniedToolIds?: Set<string>;

  /**
   * Update streaming enabled configuration - 接收流式传输配置
   */
  updateStreamingEnabled?: (json: string) => void;

  /**
   * Rewind result callback - 回滚操作结果回调
   */
  onRewindResult?: (json: string) => void;

  /**
   * Undo file result callback - 单文件撤销操作结果回调
   */
  onUndoFileResult?: (json: string) => void;

  /**
   * Undo all files result callback - 批量撤销操作结果回调
   */
  onUndoAllFileResult?: (json: string) => void;

  // ============================================================================
  // 🔧 依赖管理回调函数
  // ============================================================================

  /**
   * Update dependency status callback - 更新依赖状态
   */
  updateDependencyStatus?: (json: string) => void;

  /**
   * Dependency install progress callback - 依赖安装进度
   */
  dependencyInstallProgress?: (json: string) => void;

  /**
   * Dependency install result callback - 依赖安装结果
   */
  dependencyInstallResult?: (json: string) => void;

  /**
   * Dependency uninstall result callback - 依赖卸载结果
   */
  dependencyUninstallResult?: (json: string) => void;

  /**
   * Node environment status callback - Node.js 环境状态
   */
  nodeEnvironmentStatus?: (json: string) => void;

  /**
   * Dependency update available callback - 依赖更新检查结果
   */
  dependencyUpdateAvailable?: (json: string) => void;

  /**
   * Pending dependency updates payload before settings initialization
   */
  __pendingDependencyUpdates?: string;

  /**
   * Pending dependency status payload before React initialization
   */
  __pendingDependencyStatus?: string;

  /**
   * Pending streaming enabled status before React initialization
   */
  __pendingStreamingEnabled?: string;

  /**
   * Pending send shortcut status before React initialization
   */
  __pendingSendShortcut?: string;

  __pendingPermissionDialogRequests?: string[];

  __pendingAskUserQuestionDialogRequests?: string[];

  __pendingPlanApprovalDialogRequests?: string[];

  /**
   * Pending user message before addUserMessage is registered (for Quick Fix feature)
   */
  __pendingUserMessage?: string;

  /**
   * Pending loading state before showLoading is registered (for Quick Fix feature)
   */
  __pendingLoadingState?: boolean;
}

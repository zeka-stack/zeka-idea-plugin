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
   * Clear all messages
   */
  clearMessages?: () => void;

  /**
   * Add error message
   */
  addErrorMessage?: (message: string) => void;

  /**
   * Add user message to chat (used for external Quick Fix feature)
   * Immediately shows the user's message in the chat UI before AI response
   */
  addUserMessage?: (content: string) => void;

  /**
   * Add toast notification (called from backend)
   */
  addToast?: (message: string, type: 'success' | 'error' | 'warning' | 'info') => void;

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
   * Update active provider
   */
  updateActiveProvider?: (providerId: string) => void;

  /**
   * Update thinking enabled configuration
   */
  updateThinkingEnabled?: (json: string) => void;

  /**
   * Update streaming enabled configuration
   */
  updateStreamingEnabled?: (json: string) => void;

  /**
   * Update send shortcut setting
   */
  updateSendShortcut?: (json: string) => void;

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
   * IDE theme received callback - 接收 IDE 主题配置
   */
  onIdeThemeReceived?: (json: string) => void;

  /**
   * IDE theme changed callback - IDE 主题变化时的回调
   */
  onIdeThemeChanged?: (json: string) => void;

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
   * Pending streaming enabled status before React initialization
   */
  __pendingStreamingEnabled?: string;

  /**
   * Pending send shortcut status before React initialization
   */
  __pendingSendShortcut?: string;
}

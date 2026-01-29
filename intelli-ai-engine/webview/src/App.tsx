import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {sendBridgeEvent} from './utils/bridge';
import {ChatInputBox} from './components/ChatInputBox';
import type {ContextInfo} from './hooks';
import {useScrollBehavior, useStreamingMessages, useWindowCallbacks,} from './hooks';
import {createLocalizeMessage} from './utils/localizationUtils';
import {
    getContentBlocks as getContentBlocksUtil,
    getMessageText as getMessageTextUtil,
    mergeConsecutiveAssistantMessages,
    normalizeBlocks as normalizeBlocksUtil,
    shouldShowMessage as shouldShowMessageUtil,
} from './utils/messageUtils';
import type {Attachment, ChatInputBoxHandle, ModelInfo, ProviderInfo} from './components/ChatInputBox/types';
import {ToastContainer, type ToastMessage} from './components/Toast';
import {ScrollControl} from './components/ScrollControl';
import {extractMarkdownContent} from './utils/copyUtils';
import {ChatHeader} from './components/ChatHeader';
import {WelcomeScreen} from './components/WelcomeScreen';
import {MessageList} from './components/MessageList';
import type {ClaudeContentBlock, ClaudeMessage, ClaudeRawMessage,} from './types';
import type {ProviderConfig} from './types/provider';

const DEFAULT_STATUS = 'ready';


const App = () => {
  const { t } = useTranslation();

  const [messages, setMessages] = useState<ClaudeMessage[]>([]);
  const [_status, setStatus] = useState(DEFAULT_STATUS); // Internal state, displayed via toast
  const [loading, setLoading] = useState(false);
  const [loadingStartTime, setLoadingStartTime] = useState<number | null>(null);
  const [isThinking, setIsThinking] = useState(false);
  const [streamingActive, setStreamingActive] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  // IDE 主题状态 - 优先使用 Java 注入的初始主题
  const [ideTheme, setIdeTheme] = useState<'light' | 'dark' | null>(() => {
    // 检查 Java 是否注入了初始主题
    const injectedTheme = (window as any).__INITIAL_IDE_THEME__;
    if (injectedTheme === 'light' || injectedTheme === 'dark') {
      return injectedTheme;
    }
    return null;
  });
  // Scroll behavior management
  const {
    messagesContainerRef,
    messagesEndRef,
    inputAreaRef,
    isUserAtBottomRef,
  } = useScrollBehavior({
    messages,
    loading,
    streamingActive,
  });

  // Streaming message state and helpers
  const {
    streamingContentRef,
    isStreamingRef,
    useBackendStreamingRenderRef,
    streamingMessageIndexRef,
    streamingTextSegmentsRef,
    activeTextSegmentIndexRef,
    streamingThinkingSegmentsRef,
    activeThinkingSegmentIndexRef,
    seenToolUseCountRef,
    contentUpdateTimeoutRef,
    thinkingUpdateTimeoutRef,
    lastContentUpdateRef,
    lastThinkingUpdateRef,
    autoExpandedThinkingKeysRef,
    findLastAssistantIndex,
    extractRawBlocks,
    getOrCreateStreamingAssistantIndex,
    patchAssistantForStreaming,
  } = useStreamingMessages();

  // ============================================================
  // Performance optimization: Use ref for ChatInputBox instead of controlled mode
  // This eliminates re-render loops caused by value/onInput sync
  // ============================================================
  const chatInputRef = useRef<ChatInputBoxHandle>(null);
  // Keep draftInput for backward compatibility (still used in some places)
  // but now it's updated via debounced callback, not on every keystroke
  const [draftInput, setDraftInput] = useState('');

  // ChatInputBox 相关状态
  const [currentProvider, setCurrentProvider] = useState('');
  const [selectedClaudeModel, setSelectedClaudeModel] = useState('');
  const [selectedCodexModel, setSelectedCodexModel] = useState('');
  const [availableProviders, setAvailableProviders] = useState<ProviderInfo[]>([]);
  const [providerModels, setProviderModels] = useState<Record<string, ModelInfo[]>>({});
  const [, setProviderConfigVersion] = useState(0);
  const [activeProviderConfig, setActiveProviderConfig] = useState<ProviderConfig | null>(null);
  const [claudeSettingsAlwaysThinkingEnabled, setClaudeSettingsAlwaysThinkingEnabled] = useState(true);
  // 🔧 流式传输开关状态（同步设置页面）
  const [streamingEnabledSetting, setStreamingEnabledSetting] = useState(true);
  // 发送快捷键设置
  const [sendShortcut, setSendShortcut] = useState<'enter' | 'cmdEnter'>('enter');

  // 使用 useRef 存储最新的 provider 值，避免回调中的闭包问题
  const currentProviderRef = useRef(currentProvider);
  useEffect(() => {
    currentProviderRef.current = currentProvider;
  }, [currentProvider]);

  // Engine providers from backend
  useEffect(() => {
    const updateProviders = (jsonStr: string) => {
      try {
        const payload = JSON.parse(jsonStr);
        const list = Array.isArray(payload) ? payload : payload.providers;
        if (!Array.isArray(list)) return;

        const providers: ProviderInfo[] = list.map((item: any) => ({
          id: String(item.id ?? ''),
          label: String(item.label ?? item.name ?? item.providerType ?? item.id ?? ''),
          icon: 'codicon-hubot',
          enabled: true,
        })).filter((p: ProviderInfo) => p.id);

        const modelsMap: Record<string, ModelInfo[]> = {};
        let activeId = '';
        let activeModel = '';

        for (const item of list) {
          const id = String(item.id ?? '');
          if (!id) continue;
          const models = Array.isArray(item.models) ? item.models : [];
          const modelList = models.length > 0 ? models : (item.model ? [item.model] : []);
          const modelInfos = modelList.map((m: string) => ({ id: m, label: m }));
          modelsMap[id] = modelInfos;

          if (item.isActive || item.active) {
            activeId = id;
            activeModel = item.model || modelList[0] || '';
          }
        }

        setAvailableProviders(providers);
        setProviderModels(modelsMap);

        const fallbackProvider = activeId || providers[0]?.id || '';
        if (fallbackProvider && fallbackProvider !== currentProviderRef.current) {
          setCurrentProvider(fallbackProvider);
        }

        const fallbackModel = activeModel || modelsMap[fallbackProvider]?.[0]?.id || '';
        if (fallbackModel) {
          setSelectedClaudeModel(fallbackModel);
          setSelectedCodexModel(fallbackModel);
        }
      } catch (error) {
        console.error('[Frontend] Failed to parse engine providers:', error);
      }
    };

    window.updateEngineProviders = updateProviders;
    sendBridgeEvent('get_providers');

    return () => {
      delete window.updateEngineProviders;
    };
  }, []);

  // 根据当前提供商选择显示的模型
  const selectedModel = selectedClaudeModel || selectedCodexModel;

  // Context state (active file and selection) - 保留用于 ContextBar 显示
  const [contextInfo, setContextInfo] = useState<ContextInfo | null>(null);

  const currentModels = useMemo(() => {
    return providerModels[currentProvider] || [];
  }, [providerModels, currentProvider]);

  useEffect(() => {
    if (currentModels.length === 0) {
      return;
    }
    const currentModelId = selectedModel;
    const hasModel = currentModels.some((m) => m.id === currentModelId);
    if (!hasModel) {
      const fallback = currentModels[0].id;
      setSelectedClaudeModel(fallback);
      setSelectedCodexModel(fallback);
    }
  }, [currentModels, selectedModel]);

  const syncActiveProviderModelMapping = (_provider?: ProviderConfig | null) => {};

  // 全局拖拽事件拦截 - 阻止浏览器默认的文件打开行为
  // 这确保拖拽文件到插件任意位置都不会触发浏览器打开文件
  useEffect(() => {
    const preventDefaultDragDrop = (e: DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
    };

    // 在 document 级别拦截所有 dragover 和 drop 事件
    document.addEventListener('dragover', preventDefaultDragDrop);
    document.addEventListener('drop', preventDefaultDragDrop);
    // 同时处理 dragenter 和 dragleave 以防止任何意外行为
    document.addEventListener('dragenter', preventDefaultDragDrop);

    return () => {
      document.removeEventListener('dragover', preventDefaultDragDrop);
      document.removeEventListener('drop', preventDefaultDragDrop);
      document.removeEventListener('dragenter', preventDefaultDragDrop);
    };
  }, []);

  // 初始化主题和字体缩放
  useEffect(() => {
    // 注册 IDE 主题接收回调
    window.onIdeThemeReceived = (jsonStr: string) => {
      try {
        const themeData = JSON.parse(jsonStr);
        const theme = themeData.isDark ? 'dark' : 'light';
        setIdeTheme(theme);
      } catch {
        // Failed to parse IDE theme response
      }
    };

    // 监听 IDE 主题变化（当用户在 IDE 中切换主题时）
    window.onIdeThemeChanged = (jsonStr: string) => {
      try {
        const themeData = JSON.parse(jsonStr);
        const theme = themeData.isDark ? 'dark' : 'light';
        setIdeTheme(theme);
      } catch {
        // Failed to parse IDE theme change
      }
    };

    // 初始化字体缩放
    const savedLevel = localStorage.getItem('fontSizeLevel');
    const level = savedLevel ? parseInt(savedLevel, 10) : 2; // 默认档位 2 (90%)
    const fontSizeLevel = (level >= 1 && level <= 6) ? level : 2;

    // 将档位映射到缩放比例
    const fontSizeMap: Record<number, number> = {
      1: 0.8,   // 80%
      2: 0.9,   // 90% (默认)
      3: 1.0,   // 100%
      4: 1.1,   // 110%
      5: 1.2,   // 120%
      6: 1.4,   // 140%
    };
    const scale = fontSizeMap[fontSizeLevel] || 1.0;
    document.documentElement.style.setProperty('--font-scale', scale.toString());

    // 先应用用户明确选择的主题（light/dark），跟随 IDE 的情况等 ideTheme 更新后再处理
    const savedTheme = localStorage.getItem('theme');

    // 检查是否有 Java 注入的初始主题
    const injectedTheme = (window as any).__INITIAL_IDE_THEME__;

    // 请求 IDE 主题（带重试机制）- 仍然需要，用于处理动态主题变化
    let retryCount = 0;
    const MAX_RETRIES = 20; // 最多重试 20 次 (2 秒)

    const requestIdeTheme = () => {
      if (window.sendToJava) {
        window.sendToJava('get_ide_theme:');
      } else {
        retryCount++;
        if (retryCount < MAX_RETRIES) {
          setTimeout(requestIdeTheme, 100);
        } else {
          // 如果是 Follow IDE 模式且无法获取 IDE 主题，使用注入的主题或 dark 作为 fallback
          if (savedTheme === null || savedTheme === 'system') {
            const fallback = injectedTheme || 'dark';
            setIdeTheme(fallback as 'light' | 'dark');
          }
        }
      }
    };

    // 延迟 100ms 开始请求，给 bridge 初始化时间
    setTimeout(requestIdeTheme, 100);
  }, []);

  // 当 IDE 主题变化时，重新应用主题（如果用户选择了"跟随 IDE"）
  // 这个 effect 也处理初始加载时的主题设置
  useEffect(() => {
    const savedTheme = localStorage.getItem('theme');

    // 只有在 ideTheme 已加载后才处理
    if (ideTheme === null) {
      return;
    }

    // 如果用户选择了 "Follow IDE" 模式
    if (savedTheme === null || savedTheme === 'system') {
      document.documentElement.setAttribute('data-theme', ideTheme);
    }
  }, [ideTheme]);

  // Engine 版本不使用本地模型缓存，模型由后端统一下发

  // Toast helper functions
  const addToast = (message: string, type: ToastMessage['type'] = 'info') => {
    // Don't show toast for default status
    if (message === DEFAULT_STATUS || !message) return;

    const id = `toast-${Date.now()}-${Math.random()}`;
    setToasts((prev) => [...prev, { id, message, type }]);
  };

  const dismissToast = (id: string) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  };

  // Window callbacks (bridge communication)
  useWindowCallbacks({
    t,
    addToast,
    setMessages,
    setStatus,
    setLoading,
    setLoadingStartTime,
    setIsThinking,
    setStreamingActive,
    setSelectedClaudeModel,
    setSelectedCodexModel,
    setProviderConfigVersion,
    setActiveProviderConfig,
    setClaudeSettingsAlwaysThinkingEnabled,
    setStreamingEnabledSetting,
    setSendShortcut,
    setContextInfo,
    currentProviderRef,
    messagesContainerRef,
    isUserAtBottomRef,
    streamingContentRef,
    isStreamingRef,
    useBackendStreamingRenderRef,
    autoExpandedThinkingKeysRef,
    streamingTextSegmentsRef,
    activeTextSegmentIndexRef,
    streamingThinkingSegmentsRef,
    activeThinkingSegmentIndexRef,
    seenToolUseCountRef,
    streamingMessageIndexRef,
    lastContentUpdateRef,
    contentUpdateTimeoutRef,
    lastThinkingUpdateRef,
    thinkingUpdateTimeoutRef,
    findLastAssistantIndex,
    extractRawBlocks,
    getOrCreateStreamingAssistantIndex,
    patchAssistantForStreaming,
    syncActiveProviderModelMapping,
  });

  /**
   * 构建用户消息的内容块
   */
  const buildUserContentBlocks = useCallback((
    text: string,
    attachments: Attachment[] | undefined
  ): ClaudeContentBlock[] => {
    const blocks: ClaudeContentBlock[] = [];

    if (Array.isArray(attachments) && attachments.length > 0) {
      for (const att of attachments) {
        if (att.mediaType?.startsWith('image/')) {
          blocks.push({
            type: 'image',
            src: `data:${att.mediaType};base64,${att.data}`,
            mediaType: att.mediaType,
          });
        } else {
          blocks.push({
            type: 'text',
            text: t('chat.attachmentFile', { fileName: att.fileName }),
          });
        }
      }
    }

    if (text) {
      blocks.push({ type: 'text', text });
    }

    return blocks;
  }, [t]);

  /**
   * 发送消息到后端
   */
  const sendMessageToBackend = useCallback((
    text: string,
    attachments: Attachment[] | undefined,
    fileTagsInfo: { displayPath: string; absolutePath: string }[] | null
  ) => {
    const hasAttachments = Array.isArray(attachments) && attachments.length > 0;

    if (hasAttachments) {
      try {
        const payload = JSON.stringify({
          text,
          attachments: (attachments || []).map(a => ({
            fileName: a.fileName,
            mediaType: a.mediaType,
            data: a.data,
          })),
          fileTags: fileTagsInfo,
        });
        sendBridgeEvent('send_message_with_attachments', payload);
      } catch (error) {
        console.error('[Frontend] Failed to serialize attachments payload', error);
        const fallbackPayload = JSON.stringify({ text, fileTags: fileTagsInfo });
        sendBridgeEvent('send_message', fallbackPayload);
      }
    } else {
      const payload = JSON.stringify({ text, fileTags: fileTagsInfo });
      sendBridgeEvent('send_message', payload);
    }
  }, []);

  /**
   * 处理消息发送（来自 ChatInputBox）
   */
  const handleSubmit = (content: string, attachments?: Attachment[]) => {
    const text = content.replace(/[\u200B-\u200D\uFEFF]/g, '').trim();
    const hasAttachments = Array.isArray(attachments) && attachments.length > 0;

    // 验证输入
    if (!text && !hasAttachments) return;
    if (loading) return;

    // 构建用户消息内容块
    const userContentBlocks = buildUserContentBlocks(text, attachments);
    if (userContentBlocks.length === 0) return;

    // 创建并添加用户消息（乐观更新）
    const userMessage: ClaudeMessage = {
      type: 'user',
      content: text || (hasAttachments ? t('chat.attachmentsUploaded') : ''),
      timestamp: new Date().toISOString(),
      isOptimistic: true,
      raw: { message: { content: userContentBlocks } },
    };
    setMessages((prev) => [...prev, userMessage]);

    // 设置 loading 状态
    setLoading(true);
    setLoadingStartTime(Date.now());

    // 滚动到底部
    isUserAtBottomRef.current = true;
    requestAnimationFrame(() => {
      if (messagesContainerRef.current) {
        messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
      }
    });

    // 同步 provider 设置
    if (currentProvider) {
      sendBridgeEvent('set_provider', currentProvider);
    }
    if (selectedModel) {
      sendBridgeEvent('set_model', selectedModel);
    }

    // 提取文件标签信息
    const fileTags = chatInputRef.current?.getFileTags() ?? [];
    const fileTagsInfo = fileTags.length > 0 ? fileTags.map(tag => ({
      displayPath: tag.displayPath,
      absolutePath: tag.absolutePath,
    })) : null;

    const extractedFileTags = fileTagsInfo ?? (() => {
      const matches = text.match(/@\/(?:[^\s'\"<>])+|@~\/(?:[^\s'\"<>])+|@[A-Za-z]:\\\\(?:[^\s'\"<>])+/g);
      if (!matches || matches.length === 0) return null;
      const normalized = matches.map((raw) => {
        const path = raw.startsWith('@') ? raw.slice(1) : raw;
        return { displayPath: path, absolutePath: path };
      });
      return normalized.length > 0 ? normalized : null;
    })();

    // 发送消息到后端
    sendMessageToBackend(text, attachments, extractedFileTags);
  };

  /**
   * 处理模型选择
   */
  const handleModelSelect = (modelId: string) => {
    setSelectedClaudeModel(modelId);
    setSelectedCodexModel(modelId);
    sendBridgeEvent('set_model', modelId);
  };

  /**
   * 处理提供商选择
   * 切换 provider 时清空消息和输入框（类似新建会话）
   */
  const handleProviderSelect = (providerId: string) => {
    // 清空消息列表（类似新建会话）
    setMessages([]);
    // 清空输入框
    chatInputRef.current?.clear();

    setCurrentProvider(providerId);
    sendBridgeEvent('set_provider', providerId);
    // 切换 provider 时,同时发送对应的模型
    const models = providerModels[providerId] || [];
    const newModel = models[0]?.id || selectedModel;
    if (newModel) {
      setSelectedClaudeModel(newModel);
      setSelectedCodexModel(newModel);
      sendBridgeEvent('set_model', newModel);
    }
  };

  /**
   * 处理思考模式切换
   */
  const handleToggleThinking = (enabled: boolean) => {
    if (!activeProviderConfig) {
      setClaudeSettingsAlwaysThinkingEnabled(enabled);
      sendBridgeEvent('set_thinking_enabled', JSON.stringify({ enabled }));
      addToast(enabled ? t('toast.thinkingEnabled') : t('toast.thinkingDisabled'), 'success');
      return;
    }

    // 更新本地状态（乐观更新）
    setActiveProviderConfig(prev => prev ? {
      ...prev,
      settingsConfig: {
        ...prev.settingsConfig,
        alwaysThinkingEnabled: enabled
      }
    } : null);

    // 发送更新到后端
    const payload = JSON.stringify({
      id: activeProviderConfig.id,
      updates: {
        settingsConfig: {
          ...(activeProviderConfig.settingsConfig || {}),
          alwaysThinkingEnabled: enabled
        }
      }
    });
    sendBridgeEvent('update_provider', payload);
    addToast(enabled ? t('toast.thinkingEnabled') : t('toast.thinkingDisabled'), 'success');
  };

  /**
   * 处理流式传输开关切换
   */
  const handleStreamingEnabledChange = useCallback((enabled: boolean) => {
    setStreamingEnabledSetting(enabled);
    const payload = { streamingEnabled: enabled };
    sendBridgeEvent('set_streaming_enabled', JSON.stringify(payload));
    addToast(enabled ? t('settings.basic.streaming.enabled') : t('settings.basic.streaming.disabled'), 'success');
  }, [t, addToast]);

  const interruptSession = () => {
    // FIX: 立即重置前端状态，不等待后端回调
    // 这样可以让用户立即看到停止效果
    setLoading(false);
    setLoadingStartTime(null);
    setStreamingActive(false);
    isStreamingRef.current = false;

    sendBridgeEvent('interrupt_session');
  };

  // Message utility functions (use imported utilities with bound dependencies)
  const localizeMessage = useMemo(() => createLocalizeMessage(t), [t]);

  // Cache for normalizeBlocks to avoid re-parsing unchanged messages
  const normalizeBlocksCache = useRef(new WeakMap<object, ClaudeContentBlock[]>());
  const shouldShowMessageCache = useRef(new WeakMap<object, boolean>());
  const mergedAssistantMessageCache = useRef(new Map<string, { source: ClaudeMessage[]; merged: ClaudeMessage }>());
  // Clear cache when dependencies change
  useEffect(() => {
    normalizeBlocksCache.current = new WeakMap();
    shouldShowMessageCache.current = new WeakMap();
    mergedAssistantMessageCache.current = new Map();
  }, [localizeMessage, t]);

  const normalizeBlocks = useCallback(
    (raw?: ClaudeRawMessage | string) => {
      if (!raw) return null;
      if (typeof raw === 'object') {
        const cache = normalizeBlocksCache.current;
        if (cache.has(raw)) {
          return cache.get(raw)!;
        }
        const result = normalizeBlocksUtil(raw, localizeMessage, t);
        if (result) {
          cache.set(raw, result);
        }
        return result;
      }
      return normalizeBlocksUtil(raw, localizeMessage, t);
    },
    [localizeMessage, t]
  );

  const getMessageText = useCallback(
    (message: ClaudeMessage) => getMessageTextUtil(message, localizeMessage, t),
    [localizeMessage, t]
  );

  const shouldShowMessage = useCallback(
    (message: ClaudeMessage) => shouldShowMessageUtil(message, getMessageText, normalizeBlocks, t),
    [getMessageText, normalizeBlocks, t]
  );

  const shouldShowMessageCached = useCallback(
    (message: ClaudeMessage) => {
      const cache = shouldShowMessageCache.current;
      if (cache.has(message)) {
        return cache.get(message)!;
      }
      const result = shouldShowMessage(message);
      cache.set(message, result);
      return result;
    },
    [shouldShowMessage]
  );

  const getContentBlocks = useCallback(
    (message: ClaudeMessage) => getContentBlocksUtil(message, normalizeBlocks, localizeMessage),
    [normalizeBlocks, localizeMessage]
  );

  // Merge consecutive assistant messages to fix style inconsistencies in stream rendering
  const mergedMessages = useMemo(() => {
    const visible: ClaudeMessage[] = [];
    for (const message of messages) {
      if (shouldShowMessageCached(message)) {
        visible.push(message);
      }
    }
    const result = mergeConsecutiveAssistantMessages(visible, normalizeBlocks, mergedAssistantMessageCache.current);
    return result;
  }, [messages, shouldShowMessageCached, normalizeBlocks]);

  const sessionTitle = useMemo(() => {
    if (messages.length === 0) {
      return t('common.newSession');
    }
    const firstUserMessage = messages.find((message) => message.type === 'user');
    if (!firstUserMessage) {
      return t('common.newSession');
    }
    const text = getMessageText(firstUserMessage);
    return text.length > 15 ? `${text.substring(0, 15)}...` : text;
  }, [messages, t]);

  return (
    <>
      <ToastContainer messages={toasts} onDismiss={dismissToast} />
      <ChatHeader
        sessionTitle={sessionTitle}
      />

      <div className="messages-container" ref={messagesContainerRef}>
        {messages.length === 0 && (
          <WelcomeScreen
            currentProvider={currentProvider}
            currentProviderLabel={availableProviders.find(p => p.id === currentProvider)?.label}
            t={t}
            onProviderChange={handleProviderSelect}
          />
        )}

        <MessageList
          messages={mergedMessages}
          streamingActive={streamingActive}
          isThinking={isThinking}
          loading={loading}
          loadingStartTime={loadingStartTime}
          t={t}
          getMessageText={getMessageText}
          getContentBlocks={getContentBlocks}
          extractMarkdownContent={extractMarkdownContent}
          messagesEndRef={messagesEndRef}
        />
      </div>

      {/* 滚动控制按钮 */}
      <ScrollControl containerRef={messagesContainerRef} inputAreaRef={inputAreaRef} />

      <div className="input-area" ref={inputAreaRef}>
        <ChatInputBox
          ref={chatInputRef}
          isLoading={loading}
          selectedModel={selectedModel}
          models={currentModels}
          currentProvider={currentProvider}
          providers={availableProviders}
          alwaysThinkingEnabled={activeProviderConfig?.settingsConfig?.alwaysThinkingEnabled ?? claudeSettingsAlwaysThinkingEnabled}
          placeholder={sendShortcut === 'cmdEnter' ? t('chat.inputPlaceholderCmdEnter') : t('chat.inputPlaceholderEnter')}
          // Performance optimization: Keep value for initial sync, but onInput is now debounced
          value={draftInput}
          onInput={setDraftInput}
          onSubmit={handleSubmit}
          onStop={interruptSession}
          onModelSelect={handleModelSelect}
          onProviderSelect={handleProviderSelect}
          onToggleThinking={handleToggleThinking}
          streamingEnabled={streamingEnabledSetting}
          onStreamingEnabledChange={handleStreamingEnabledChange}
          sendShortcut={sendShortcut}
          activeFile={contextInfo?.file}
          selectedLines={contextInfo?.startLine !== undefined && contextInfo?.endLine !== undefined
            ? (contextInfo.startLine === contextInfo.endLine
                ? `L${contextInfo.startLine}`
                : `L${contextInfo.startLine}-${contextInfo.endLine}`)
            : undefined}
          onClearContext={() => setContextInfo(null)}
          addToast={addToast}
        />
      </div>

      <div id="image-preview-root" />
    </>
  );
};

export default App;

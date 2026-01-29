import {memo, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import type {TFunction} from 'i18next';
import type {ClaudeContentBlock, ClaudeMessage} from '../../types';

import MarkdownBlock from '../MarkdownBlock';
import {ContentBlockRenderer} from './ContentBlockRenderer';
import {formatTime} from '../../utils/helpers';
import {copyToClipboard} from '../../utils/copyUtils';

export interface MessageItemProps {
  message: ClaudeMessage;
  messageIndex: number;
  isLast: boolean;
  streamingActive: boolean;
  isThinking: boolean;
  t: TFunction;
  getMessageText: (message: ClaudeMessage) => string;
  getContentBlocks: (message: ClaudeMessage) => ClaudeContentBlock[];
  extractMarkdownContent: (message: ClaudeMessage) => string;
}

export const MessageItem = memo(function MessageItem({
  message,
  messageIndex,
  isLast,
  streamingActive,
  isThinking,
  t,
  getMessageText,
  getContentBlocks,
  extractMarkdownContent,
}: MessageItemProps): React.ReactElement {
  const [copiedMessageIndex, setCopiedMessageIndex] = useState<number | null>(null);
  const [showStreamingConnectHint, setShowStreamingConnectHint] = useState(false);
  const [thinkingElapsedSeconds, setThinkingElapsedSeconds] = useState(0);
  const [showThinkingTimer, setShowThinkingTimer] = useState(false);

  // Track timeout to properly cleanup on unmount
  const copyTimeoutRef = useRef<number | null>(null);
  const thinkingStartRef = useRef<number | null>(null);
  const thinkingTimerRef = useRef<number | null>(null);

  // Manage thinking expansion state locally to avoid prop drilling and unnecessary re-renders
  const [expandedThinking, setExpandedThinking] = useState<Record<number, boolean>>({});

  const toggleThinking = useCallback((blockIndex: number) => {
    setExpandedThinking((prev) => ({
      ...prev,
      [blockIndex]: !prev[blockIndex],
    }));
  }, []);

  const isThinkingExpanded = useCallback(
    (blockIndex: number) => Boolean(expandedThinking[blockIndex]),
    [expandedThinking]
  );

  const isLastAssistantMessage = message.type === 'assistant' && isLast;
  const isMessageStreaming = streamingActive && isLastAssistantMessage;

  // Cache markdown content extraction for better performance
  const markdownContent = useMemo(() => {
    // Only extract for user and assistant messages that need copy functionality
    if (message.type === 'user' || message.type === 'assistant') {
      return extractMarkdownContent(message);
    }
    return '';
  }, [message, extractMarkdownContent]);

  const handleCopyMessage = useCallback(async () => {
    // Prevent copying if message is empty or already in "copied" state
    if (!markdownContent.trim() || copiedMessageIndex === messageIndex) return;

    const success = await copyToClipboard(markdownContent);
    if (success) {
      setCopiedMessageIndex(messageIndex);

      // Clear any existing timeout before setting new one
      if (copyTimeoutRef.current !== null) {
        window.clearTimeout(copyTimeoutRef.current);
      }

      // Set new timeout and store ID for cleanup
      copyTimeoutRef.current = window.setTimeout(() => {
        setCopiedMessageIndex(null);
        copyTimeoutRef.current = null;
      }, 1500);
    }
  }, [markdownContent, messageIndex, copiedMessageIndex]);

  // Cleanup timeout on unmount to prevent memory leaks
  useEffect(() => {
    return () => {
      if (copyTimeoutRef.current !== null) {
        window.clearTimeout(copyTimeoutRef.current);
        copyTimeoutRef.current = null;
      }
    };
  }, []);

  // Memoize blocks and grouped blocks to avoid recalculation on every render
  const blocks = useMemo(() => getContentBlocks(message), [message, getContentBlocks]);
  const hasThinkingBlock = useMemo(() => blocks.some((block) => block.type === 'thinking'), [blocks]);
  const hasAnswerContent = useMemo(
    () => blocks.some((block) => block.type !== 'thinking'),
    [blocks]
  );
  const isEmptyStreamingPlaceholder =
    message.type === 'assistant' &&
    isMessageStreaming &&
    blocks.length === 0 &&
    !(message.content && message.content.trim().length > 0);

  useEffect(() => {
    if (!isEmptyStreamingPlaceholder) {
      setShowStreamingConnectHint(false);
      return;
    }
    const timer = window.setTimeout(() => setShowStreamingConnectHint(true), 350);
    return () => window.clearTimeout(timer);
  }, [isEmptyStreamingPlaceholder]);

  // Ref to track the last auto-expanded thinking block index to avoid overriding user interaction
  const lastAutoExpandedIndexRef = useRef<number>(-1);

  // Auto-expand the latest thinking block during streaming
  useEffect(() => {
    if (!isMessageStreaming || hasAnswerContent) return;

    const thinkingIndices = blocks
      .map((block, index) => (block.type === 'thinking' ? index : -1))
      .filter((index) => index !== -1);

    if (thinkingIndices.length === 0) return;

    const lastThinkingIndex = thinkingIndices[thinkingIndices.length - 1];

    if (lastThinkingIndex !== lastAutoExpandedIndexRef.current) {
      setExpandedThinking((prev) => {
        const newState = { ...prev };
        // Collapse all thinking blocks
        thinkingIndices.forEach((idx) => {
          newState[idx] = false;
        });
        // Expand the latest one
        newState[lastThinkingIndex] = true;
        return newState;
      });
      lastAutoExpandedIndexRef.current = lastThinkingIndex;
    }
  }, [blocks, isMessageStreaming, hasAnswerContent]);

  // Collapse thinking when real answer starts
  useEffect(() => {
    if (!isMessageStreaming || !hasAnswerContent) return;
    const thinkingIndices = blocks
      .map((block, index) => (block.type === 'thinking' ? index : -1))
      .filter((index) => index !== -1);
    if (thinkingIndices.length === 0) return;
    setExpandedThinking((prev) => {
      const next = { ...prev };
      thinkingIndices.forEach((idx) => {
        next[idx] = false;
      });
      return next;
    });
    lastAutoExpandedIndexRef.current = -1;
  }, [blocks, hasAnswerContent, isMessageStreaming]);

  const stopThinkingTimer = useCallback(() => {
    if (thinkingTimerRef.current !== null) {
      window.clearInterval(thinkingTimerRef.current);
      thinkingTimerRef.current = null;
    }
    if (thinkingStartRef.current !== null) {
      const elapsed = Math.floor((Date.now() - thinkingStartRef.current) / 1000);
      setThinkingElapsedSeconds(elapsed);
    }
  }, []);

  // Reset thinking timer when message changes
  useEffect(() => {
    if (thinkingTimerRef.current !== null) {
      window.clearInterval(thinkingTimerRef.current);
      thinkingTimerRef.current = null;
    }
    thinkingStartRef.current = null;
    setThinkingElapsedSeconds(0);
    setShowThinkingTimer(false);
  }, [message.timestamp]);

  // Track thinking elapsed time for the last streaming assistant message
  useEffect(() => {
    if (!isMessageStreaming || !hasThinkingBlock) {
      setShowThinkingTimer(false);
      stopThinkingTimer();
      return;
    }

    // Stop when formal answer starts, but keep timer visible
    if (hasAnswerContent) {
      setShowThinkingTimer(true);
      stopThinkingTimer();
      return;
    }

    if (thinkingStartRef.current === null) {
      thinkingStartRef.current = Date.now();
      setThinkingElapsedSeconds(0);
      setShowThinkingTimer(true);
    }

    if (thinkingTimerRef.current === null) {
      thinkingTimerRef.current = window.setInterval(() => {
        if (thinkingStartRef.current !== null) {
          const elapsed = Math.floor((Date.now() - thinkingStartRef.current) / 1000);
          setThinkingElapsedSeconds(elapsed);
        }
      }, 1000);
    }

    return () => {
      stopThinkingTimer();
    };
  }, [hasAnswerContent, hasThinkingBlock, isMessageStreaming, stopThinkingTimer]);

  // Ensure timer cleanup on unmount
  useEffect(() => () => stopThinkingTimer(), [stopThinkingTimer]);

  const messageStyle = useMemo(
    () => ({ contentVisibility: 'auto', containIntrinsicSize: '0 320px' } as const),
    []
  );

  const renderBlocks = () => {
    if (message.type === 'error') {
      return <MarkdownBlock content={getMessageText(message)} />;
    }

    if (isEmptyStreamingPlaceholder) {
      return (
        <div className="streaming-connect-status">
          <span className="streaming-connect-text">{t('chat.streamingConnected')}</span>
        </div>
      );
    }

    return blocks.map((block, blockIndex) => {
      return (
        <div key={`${messageIndex}-${blockIndex}`} className="content-block">
          <ContentBlockRenderer
            block={block}
            messageType={message.type}
            isStreaming={isMessageStreaming}
            isThinkingExpanded={isThinkingExpanded(blockIndex)}
            isThinking={isThinking}
            thinkingElapsedSeconds={thinkingElapsedSeconds}
            showThinkingTimer={showThinkingTimer}
            isLastMessage={isLast}
            isLastBlock={blockIndex === blocks.length - 1}
            t={t}
            onToggleThinking={() => toggleThinking(blockIndex)}
          />
        </div>
      );
    });
  };

  if (isEmptyStreamingPlaceholder && !showStreamingConnectHint) {
    return <></>;
  }

  return (
    <div className={`message ${message.type}`} style={messageStyle}>
      {/* Copy button for user and assistant messages */}
      {(message.type === 'user' || (message.type === 'assistant' && !isMessageStreaming)) && (
        <button
          type="button"
          className={`message-copy-btn ${message.type === 'user' ? 'message-copy-btn-user' : ''} ${copiedMessageIndex === messageIndex ? 'copied' : ''}`}
          onClick={handleCopyMessage}
          title={t('markdown.copyMessage')}
          aria-label={t('markdown.copyMessage')}
        >
          <span className="copy-icon">
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M4 4l0 8a2 2 0 0 0 2 2l8 0a2 2 0 0 0 2 -2l0 -8a2 2 0 0 0 -2 -2l-8 0a2 2 0 0 0 -2 2zm2 0l8 0l0 8l-8 0l0 -8z" fill="currentColor" fillOpacity="0.9"/>
              <path d="M2 2l0 8l-2 0l0 -8a2 2 0 0 1 2 -2l8 0l0 2l-8 0z" fill="currentColor" fillOpacity="0.6"/>
            </svg>
          </span>
          <span className="copy-tooltip">{t('markdown.copySuccess')}</span>
        </button>
      )}

      {/* Timestamp for user messages */}
      {message.type === 'user' && message.timestamp && (
        <div className="message-header-row">
          <div className="message-timestamp-header">
            {formatTime(message.timestamp)}
          </div>
        </div>
      )}

      {/* Role label for non-user/assistant messages */}
      {message.type !== 'assistant' && message.type !== 'user' && (
        <div className="message-role-label">
          {message.type}
        </div>
      )}

      <div className="message-content">
        {renderBlocks()}
      </div>
    </div>
  );
});

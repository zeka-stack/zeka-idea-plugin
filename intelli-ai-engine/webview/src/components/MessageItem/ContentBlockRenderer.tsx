import type {TFunction} from 'i18next';
import type {ClaudeContentBlock} from '../../types';

import MarkdownBlock from '../MarkdownBlock';
import CollapsibleTextBlock from '../CollapsibleTextBlock';

export interface ContentBlockRendererProps {
  block: ClaudeContentBlock;
  messageType: string;
  isStreaming: boolean;
  isThinkingExpanded: boolean;
  isThinking: boolean;
  thinkingElapsedSeconds?: number;
  showThinkingTimer?: boolean;
  isLastMessage: boolean;
  isLastBlock?: boolean;
  t: TFunction;
  onToggleThinking: () => void;
}

export function ContentBlockRenderer({
  block,
  messageType,
  isStreaming,
  isThinkingExpanded,
  isThinking,
  thinkingElapsedSeconds,
  showThinkingTimer,
  isLastMessage,
  isLastBlock = false,
  t,
  onToggleThinking,
}: ContentBlockRendererProps): React.ReactElement | null {
  if (block.type === 'text') {
    return messageType === 'user' ? (
      <CollapsibleTextBlock content={block.text ?? ''} />
    ) : (
      <MarkdownBlock
        content={block.text ?? ''}
        isStreaming={isStreaming}
      />
    );
  }

  if (block.type === 'image' && block.src) {
    const handleImagePreview = () => {
      const previewRoot = document.getElementById('image-preview-root');
      if (!previewRoot || !block.src) return;

      // Clear previous content safely
      previewRoot.innerHTML = '';

      // Create overlay container
      const overlay = document.createElement('div');
      overlay.className = 'image-preview-overlay';
      overlay.onclick = () => overlay.remove();

      // Create image element safely (prevents XSS)
      const img = document.createElement('img');
      img.src = block.src;
      img.alt = t('chat.imagePreview');
      img.className = 'image-preview-content';
      img.onclick = (e) => e.stopPropagation();

      // Create close button
      const closeBtn = document.createElement('div');
      closeBtn.className = 'image-preview-close';
      closeBtn.textContent = '×';
      closeBtn.onclick = (e) => {
        e.stopPropagation();
        overlay.remove();
      };

      overlay.appendChild(img);
      overlay.appendChild(closeBtn);
      previewRoot.appendChild(overlay);
    };

    return (
      <div
        className={`message-image-block ${messageType === 'user' ? 'user-image' : ''}`}
        onClick={handleImagePreview}
        style={{ cursor: 'pointer' }}
        title={t('chat.clickToPreview')}
      >
        <img
          src={block.src}
          alt={t('chat.userUploadedImage')}
          style={{
            maxWidth: messageType === 'user' ? '200px' : '100%',
            maxHeight: messageType === 'user' ? '150px' : 'auto',
            borderRadius: '8px',
            objectFit: 'contain',
          }}
        />
      </div>
    );
  }

  if (block.type === 'thinking') {
    const rawThinking = block.thinking ?? block.text ?? t('chat.noThinkingContent');
    const thinkingContent =
      typeof rawThinking === 'string' && rawThinking.length > 0 && !/^[\r\n]/.test(rawThinking)
        ? `\n${rawThinking}`
        : rawThinking;
    const shouldShowTimer = Boolean(showThinkingTimer && isLastMessage);
    const elapsedLabel =
      typeof thinkingElapsedSeconds === 'number' ? `${thinkingElapsedSeconds}${t('common.seconds')}` : '';

    return (
      <div className="thinking-block">
        <div
          className="thinking-header"
          onClick={onToggleThinking}
        >
          <span className="thinking-title">
            <span className="thinking-emoji">🤔 </span>
            {isThinking && isLastMessage && isLastBlock
              ? t('common.thinkingProcess')
              : t('common.thinking')}
          </span>
          {shouldShowTimer && (
            <span className="thinking-timer">
              {elapsedLabel}
            </span>
          )}
          <span className="thinking-icon">
            {isThinkingExpanded ? '▼' : '▶'}
          </span>
        </div>
        <div
          className="thinking-content"
          style={{ display: isThinkingExpanded ? 'block' : 'none' }}
        >
          <MarkdownBlock
            content={normalizeMarkdownSpacing(thinkingContent)}
            isStreaming={isStreaming}
          />
        </div>
      </div>
    );
  }

  return null;
}
  const normalizeMarkdownSpacing = (value: string): string => {
    let text = value.replace(/\r\n?/g, '\n');
    const lines = text.split('\n');
    let inCode = false;
    let emptyCount = 0;
    const output: string[] = [];

    for (const line of lines) {
      if (line.startsWith("```")) {
        inCode = !inCode;
        emptyCount = 0;
        output.push(line);
        continue;
      }

      if (inCode) {
        output.push(line);
        continue;
      }

      if (line.trim().length === 0) {
        emptyCount += 1;
        if (emptyCount <= 1) {
          output.push('');
        }
        continue;
      }

      emptyCount = 0;
      output.push(line);
    }

    return output.join('\n');
  };

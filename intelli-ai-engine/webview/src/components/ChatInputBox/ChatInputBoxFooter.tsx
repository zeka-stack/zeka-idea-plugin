import type {TFunction} from 'i18next';
import type {DropdownItemData, DropdownPosition, ModelInfo, ProviderInfo} from './types.js';
import type {TooltipState} from './hooks/useTooltip.js';
import {ButtonArea} from './ButtonArea.js';
import {CompletionDropdown} from './Dropdown/index.js';

interface CompletionController {
  isOpen: boolean;
  position: DropdownPosition | null;
  items: DropdownItemData[];
  activeIndex: number;
  loading: boolean;
  close: () => void;
  selectIndex: (index: number) => void;
  handleMouseEnter: (index: number) => void;
}

export function ChatInputBoxFooter({
  disabled,
  hasInputContent,
  isLoading,
  selectedModel,
  models,
  currentProvider,
  providers,
  onSubmit,
  onStop,
  onModelSelect,
  onProviderSelect,
  alwaysThinkingEnabled,
  onToggleThinking,
  streamingEnabled,
  onStreamingEnabledChange,
  fileCompletion,
  tooltip,
  t,
}: {
  disabled: boolean;
  hasInputContent: boolean;
  isLoading: boolean;
  selectedModel: string;
  models?: ModelInfo[];
  currentProvider: string;
  providers?: ProviderInfo[];
  onSubmit: () => void;
  onStop?: () => void;
  onModelSelect?: (modelId: string) => void;
  onProviderSelect?: (providerId: string) => void;
  alwaysThinkingEnabled?: boolean;
  onToggleThinking?: (enabled: boolean) => void;
  streamingEnabled?: boolean;
  onStreamingEnabledChange?: (enabled: boolean) => void;
  fileCompletion: CompletionController;
  tooltip: TooltipState | null;
  t: TFunction;
}) {
  return (
    <>
      {/* Bottom button area */}
      <ButtonArea
        disabled={disabled || isLoading}
        hasInputContent={hasInputContent}
        isLoading={isLoading}
        selectedModel={selectedModel}
        models={models}
        currentProvider={currentProvider}
        providers={providers}
        onSubmit={onSubmit}
        onStop={onStop}
        onModelSelect={onModelSelect}
        onProviderSelect={onProviderSelect}
        alwaysThinkingEnabled={alwaysThinkingEnabled}
        onToggleThinking={onToggleThinking}
        streamingEnabled={streamingEnabled}
        onStreamingEnabledChange={onStreamingEnabledChange}
      />

      {/* @ file reference dropdown menu */}
      <CompletionDropdown
        isVisible={fileCompletion.isOpen}
        position={fileCompletion.position}
        items={fileCompletion.items}
        selectedIndex={fileCompletion.activeIndex}
        loading={fileCompletion.loading}
        emptyText={t('chat.noMatchingFiles')}
        onClose={fileCompletion.close}
        onSelect={(_, index) => fileCompletion.selectIndex(index)}
        onMouseEnter={fileCompletion.handleMouseEnter}
      />

      {/* Floating Tooltip (uses Portal or Fixed positioning to break overflow limit) */}
      {tooltip && tooltip.visible && (
        <div
          className={`tooltip-popup ${tooltip.isBar ? 'tooltip-bar' : ''}`}
          style={{
            top: `${tooltip.top}px`,
            left: `${tooltip.left}px`,
            width: tooltip.width ? `${tooltip.width}px` : undefined,
            // @ts-expect-error CSS custom properties
            '--tooltip-tx': tooltip.tx || '-50%',
            '--arrow-left': tooltip.arrowLeft || '50%',
          }}
        >
          {tooltip.text}
        </div>
      )}

    </>
  );
}

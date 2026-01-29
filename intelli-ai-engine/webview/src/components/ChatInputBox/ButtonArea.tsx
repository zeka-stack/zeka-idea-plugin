import {useCallback, useMemo} from 'react';
import {useTranslation} from 'react-i18next';
import type {ButtonAreaProps, ModelInfo} from './types';
import {ModelSelect, ProviderSelect} from './selectors';

/**
 * ButtonArea - 底部工具栏组件
 * 包含提供商选择、模型选择、发送/停止按钮
 */
export const ButtonArea = ({
  disabled = false,
  hasInputContent = false,
  isLoading = false,
  selectedModel = '',
  currentProvider = '',
  providers,
  models,
  onSubmit,
  onStop,
  onModelSelect,
  onProviderSelect,
}: ButtonAreaProps) => {
  const { t } = useTranslation();
  const availableModels = useMemo<ModelInfo[]>(() => {
    if (Array.isArray(models) && models.length > 0) {
      return models;
    }
    return [];
  }, [models]);

  /**
   * 处理提交按钮点击
   */
  const handleSubmitClick = useCallback((e: React.MouseEvent) => {
    e.stopPropagation();
    onSubmit?.();
  }, [onSubmit]);

  /**
   * 处理停止按钮点击
   */
  const handleStopClick = useCallback((e: React.MouseEvent) => {
    e.stopPropagation();
    onStop?.();
  }, [onStop]);

  /**
   * 处理模式选择
   */
  /**
   * 处理模型选择
   */
  const handleModelSelect = useCallback((modelId: string) => {
    onModelSelect?.(modelId);
  }, [onModelSelect]);

  /**
   * 处理提供商选择
   */
  const handleProviderSelect = useCallback((providerId: string) => {
    onProviderSelect?.(providerId);
  }, [onProviderSelect]);

  return (
    <div className="button-area" data-provider={currentProvider}>
      {/* 左侧：选择器 */}
      <div className="button-area-left">
        {Array.isArray(providers) && providers.length > 0 && (
          <ProviderSelect value={currentProvider} onChange={handleProviderSelect} providers={providers} />
        )}
        {availableModels.length > 0 && (
          <ModelSelect value={selectedModel} onChange={handleModelSelect} models={availableModels} currentProvider={currentProvider} />
        )}
      </div>

      {/* 右侧:工具按钮 */}
      <div className="button-area-right">
        <div className="button-divider" />

        {/* 发送/停止按钮 */}
        {isLoading ? (
          <button
            className="submit-button stop-button"
            onClick={handleStopClick}
            title={t('chat.stopGeneration')}
          >
            <span className="codicon codicon-debug-stop" />
          </button>
        ) : (
          <button
            className="submit-button"
            onClick={handleSubmitClick}
            disabled={disabled || !hasInputContent}
            title={t('chat.sendMessageEnter')}
          >
            <span className="codicon codicon-send" />
          </button>
        )}
      </div>
    </div>
  );
};

export default ButtonArea;

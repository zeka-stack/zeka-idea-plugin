import type {TFunction} from 'i18next';

import {BackIcon} from '../Icons';

export interface ChatHeaderProps {
  currentView: 'chat' | 'history' | 'settings';
  sessionTitle: string;
  t: TFunction;
  onBack: () => void;
  onNewSession: () => void;
  onNewTab: () => void;
  onHistory: () => void;
  onSettings: () => void;
  showNewSession?: boolean;
  showNewTab?: boolean;
  showHistory?: boolean;
  showSettings?: boolean;
}

export function ChatHeader({
  currentView,
  sessionTitle,
  t,
  onBack,
  onNewSession,
  onNewTab,
  onHistory,
  onSettings,
  showNewSession = true,
  showNewTab = true,
  showHistory = true,
  showSettings = true,
}: ChatHeaderProps): React.ReactElement | null {
  if (currentView === 'settings') {
    return null;
  }

  return (
    <div className="header">
      <div className="header-left">
        {currentView === 'history' ? (
          <button className="back-button" onClick={onBack} data-tooltip={t('common.back')}>
            <BackIcon /> {t('common.back')}
          </button>
        ) : (
          <div
            className="session-title"
            style={{
              fontWeight: 600,
              fontSize: '14px',
              paddingLeft: '8px',
            }}
          >
            {sessionTitle}
          </div>
        )}
      </div>
      <div className="header-right">
        {currentView === 'chat' && (
          <>
            {showNewSession && (
              <button className="icon-button" onClick={onNewSession} data-tooltip={t('common.newSession')}>
                <span className="codicon codicon-plus" />
              </button>
            )}
            {showNewTab && (
              <button
                className="icon-button"
                onClick={onNewTab}
                data-tooltip={t('common.newTab')}
              >
                <span className="codicon codicon-split-horizontal" />
              </button>
            )}
            {showHistory && (
              <button
                className="icon-button"
                onClick={onHistory}
                data-tooltip={t('common.history')}
              >
                <span className="codicon codicon-history" />
              </button>
            )}
            {showSettings && (
              <button
                className="icon-button"
                onClick={onSettings}
                data-tooltip={t('common.settings')}
              >
                <span className="codicon codicon-settings-gear" />
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
}

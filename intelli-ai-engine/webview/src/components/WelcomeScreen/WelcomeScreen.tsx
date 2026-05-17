import type {TFunction} from 'i18next';

import {BlinkingLogo} from '../BlinkingLogo';
import {AnimatedText} from '../AnimatedText';
import {APP_VERSION} from '../../version/version';
import {openBrowser} from '../../utils/bridge';

const NOT_AGENT_DOC_URL = 'https://ideaplugin.dong4j.site/engine/docs/Engine-Chat-%E4%B8%8D%E6%98%AF-Agent.md';

export interface WelcomeScreenProps {
  currentProvider: string;
  currentProviderLabel?: string;
  t: TFunction;
  onProviderChange: (provider: string) => void;
}

export function WelcomeScreen({
  currentProvider,
  currentProviderLabel,
  t,
  onProviderChange,
}: WelcomeScreenProps): React.ReactElement {
  const providerLabel = currentProviderLabel || currentProvider || 'AI';
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        color: '#555',
        gap: '16px',
      }}
    >
      <div style={{ position: 'relative', display: 'inline-block' }}>
        <BlinkingLogo provider={currentProvider} onProviderChange={onProviderChange} />
        <span className="version-tag">
          v{APP_VERSION}
        </span>
      </div>
      <div>
        <AnimatedText text={t('chat.sendMessage', { provider: providerLabel })} />
      </div>
      <button
        type="button"
        className="not-agent-link"
        onClick={() => openBrowser(NOT_AGENT_DOC_URL)}
      >
        {t('chat.notAgentLink')}
      </button>
    </div>
  );
}

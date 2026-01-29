import {useEffect, useRef, useState} from 'react';
import styles from './style.module.less';
import engineLogo from '../../assets/pluginIcon.svg';

interface BlinkingLogoProps {
  provider: string;
  onProviderChange?: (providerId: string) => void;
}

export const BlinkingLogo = ({ provider }: BlinkingLogoProps) => {
  const [displayProvider, setDisplayProvider] = useState(provider);
  const [animationState, setAnimationState] = useState<'idle' | 'closing' | 'opening'>('idle');
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (provider !== displayProvider) {
      if (animationState === 'idle') {
        setAnimationState('closing');
      } else if (animationState === 'opening') {
         // If we are opening and provider changes again, we should probably close again.
         setAnimationState('closing');
      }
      // If already closing, do nothing, let it finish closing.
    }
  }, [provider, displayProvider, animationState]);

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>;

    if (animationState === 'closing') {
      timer = setTimeout(() => {
        setDisplayProvider(provider);
        setAnimationState('opening');
      }, 200); // Match CSS transition duration
    } else if (animationState === 'opening') {
      timer = setTimeout(() => {
        setAnimationState('idle');
      }, 200);
    }

    return () => {
      if (timer) clearTimeout(timer);
    };
  }, [animationState, provider]);

  return (
    <div style={{ position: 'relative', display: 'inline-flex', flexDirection: 'column', alignItems: 'center' }}>
      <div
        ref={containerRef}
        className={`${styles.container} ${styles[animationState]}`}
        style={{ cursor: 'default' }}
      >
        <img src={engineLogo} alt="IntelliAI Engine" style={{ width: 56, height: 56 }} />
      </div>
    </div>
  );
};

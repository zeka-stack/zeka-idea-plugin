import ReactDOM from 'react-dom/client';
import App from './App';
import ErrorBoundary from './components/ErrorBoundary';
import './codicon.css';
import './styles/app.less';
import './i18n/config';
import i18n from './i18n/config';
import {sendBridgeEvent} from './utils/bridge';

// Silence console output in production (including third-party libs).
// In dev, keep console for debugging.
if (!import.meta.env.DEV) {
  const noop = () => {};
  console.log = noop;
  console.debug = noop;
  console.info = noop;
  console.warn = noop;
  console.error = noop;
}

function createBridgeHeartbeatStarter() {
  let started = false;

  return () => {
    if (started) return;
    started = true;

    let lastRafAt = Date.now();
    let rafId: number | null = null;
    const rafLoop = () => {
      lastRafAt = Date.now();
      rafId = requestAnimationFrame(rafLoop);
    };
    rafId = requestAnimationFrame(rafLoop);

    let sequence = 0;
    const intervalMs = 5000;

    let intervalId: number | null = null;
    intervalId = window.setInterval(() => {
      sequence += 1;
      const payload = JSON.stringify({
        ts: Date.now(),
        raf: lastRafAt,
        visibility: document.visibilityState,
        focus: document.hasFocus(),
        seq: sequence,
      });
      sendBridgeEvent('heartbeat', payload);
    }, intervalMs);

    const cleanup = () => {
      if (rafId !== null) {
        cancelAnimationFrame(rafId);
        rafId = null;
      }
      if (intervalId !== null) {
        window.clearInterval(intervalId);
        intervalId = null;
      }
    };

    // Explicitly cleanup timers on navigation/unload (best effort; helpful for long-running JCEF contexts).
    window.addEventListener('beforeunload', cleanup, { once: true });
    window.addEventListener('pagehide', cleanup, { once: true });

    // Cleanup on Vite HMR (dev only).
    if (import.meta.hot) {
      import.meta.hot.dispose(() => cleanup());
    }

    if (import.meta.env.DEV) {
      console.log('[Main] Bridge heartbeat enabled');
    }
  };
}

const startBridgeHeartbeat = createBridgeHeartbeatStarter();
// vConsole 调试工具
const enableVConsole =
  import.meta.env.DEV || import.meta.env.VITE_ENABLE_VCONSOLE === 'true';

if (enableVConsole) {
  void import('vconsole').then(({ default: VConsole }) => {
    new VConsole();
    // 将 vConsole 按钮移到左上角，避免遮挡右下角的发送按钮
    setTimeout(() => {
      const vcSwitch = document.getElementById('__vconsole') as HTMLElement;
      if (vcSwitch) {
        vcSwitch.style.left = '10px';
        vcSwitch.style.right = 'auto';
        vcSwitch.style.top = '10px';
        vcSwitch.style.bottom = 'auto';
      }
    }, 100);
  });
}

/**
 * 应用 IDEA 编辑器字体配置到 CSS 变量
 */
/**
 * JCEF (macOS) may occasionally render with an incorrect zoom/layout after the IDE
 * stays in background / screen-off for a while. The UI uses CSS `zoom` with an
 * inverse `vw/vh` container size to implement font scaling. If the zoom is not
 * applied correctly after resume, the container becomes smaller than the viewport,
 * leaving blank areas and causing "misalignment".
 *
 * This recovery nudges Chromium/JCEF to re-apply the expected zoom and triggers
 * a resize recalculation for components relying on window size.
 */
function setupScaleRecovery() {
  type CSSStyleDeclarationWithZoom = CSSStyleDeclaration & { zoom: string };

  const getExpectedScale = (): string => {
    const fromCss = getComputedStyle(document.documentElement).getPropertyValue('--font-scale').trim();
    if (fromCss) return fromCss;

    const savedLevel = localStorage.getItem('fontSizeLevel');
    const level = savedLevel ? parseInt(savedLevel, 10) : 3;
    const fontSizeLevel = level >= 1 && level <= 6 ? level : 3;
    const fontSizeMap: Record<number, number> = {
      1: 0.8,
      2: 0.9,
      3: 1.0,
      4: 1.1,
      5: 1.2,
      6: 1.4,
    };
    return String(fontSizeMap[fontSizeLevel] || 1.0);
  };

  let hiddenAt: number | null = null;
  let lastRecoveryAt = 0;
  let scheduled = false;
  const RECOVERY_COOLDOWN_MS = 1500;

  const forceReapply = (reason: string) => {
    const app = document.getElementById('app') as HTMLElement | null;
    const expected = getExpectedScale();

    // Re-set the CSS variable to ensure width/height calc(100vw/scale) is refreshed.
    document.documentElement.style.setProperty('--font-scale', expected);

    const computedZoom = app
      ? (getComputedStyle(app) as unknown as CSSStyleDeclarationWithZoom).zoom
      : null;
    const computedZoomNumber = typeof computedZoom === 'string' ? parseFloat(computedZoom) : Number.NaN;
    const expectedNumber = parseFloat(expected);

    const needsZoomNudge =
      !!app &&
      Number.isFinite(expectedNumber) &&
      (!Number.isFinite(computedZoomNumber) || Math.abs(computedZoomNumber - expectedNumber) > 0.01);

    if (app && needsZoomNudge) {
      const appStyle = app.style as unknown as CSSStyleDeclarationWithZoom;
      // Toggle inline zoom to ensure Chromium/JCEF re-applies scaling after resume.
      // Keep the final value aligned with the CSS variable.
      appStyle.zoom = '1';
      // Force a sync layout.
      void app.offsetHeight;
      appStyle.zoom = expected;
    }

    // Let components recompute layout (some rely on window resize).
    requestAnimationFrame(() => {
      window.dispatchEvent(new Event('resize'));
      if (app && needsZoomNudge) {
        const appStyle = app.style as unknown as CSSStyleDeclarationWithZoom;
        // One more tick to reduce flakiness on macOS/JCEF.
        appStyle.zoom = expected;
      }
      if (import.meta.env.DEV) {
        console.log('[ScaleRecovery] Applied scale recovery:', {
          reason,
          expected,
          computedZoom,
          needsZoomNudge,
        });
      }
      lastRecoveryAt = Date.now();
    });
  };

  const schedule = (reason: string) => {
    if (scheduled || Date.now() - lastRecoveryAt < RECOVERY_COOLDOWN_MS) return;
    scheduled = true;
    requestAnimationFrame(() => {
      scheduled = false;
      forceReapply(reason);
    });
  };

  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      hiddenAt = Date.now();
      return;
    }

    const elapsed = hiddenAt ? Date.now() - hiddenAt : 0;
    hiddenAt = null;
    // Only nudge after a meaningful pause to avoid unnecessary work during normal tab switches.
    if (elapsed > 1500) {
      schedule('visibilitychange-resume');
    }
  });

  window.addEventListener('focus', () => {
    // Focus can return without a visibilitychange in some IDE/window states.
    schedule('window-focus');
  });

  window.addEventListener('pageshow', () => {
    // Helps if the page is restored from bfcache-like behavior.
    schedule('pageshow');
  });
}

function applyFontConfig(config: { fontFamily: string; fontSize: number; lineSpacing: number; fallbackFonts?: string[] }) {
  const root = document.documentElement;

  // 构建字体族字符串，包含主字体、回落字体和系统默认回落
  const fontParts: string[] = [`'${config.fontFamily}'`];

  // 添加 IDEA 配置的回落字体
  if (config.fallbackFonts && config.fallbackFonts.length > 0) {
    for (const fallback of config.fallbackFonts) {
      fontParts.push(`'${fallback}'`);
    }
  }

  // 添加系统默认回落字体
  fontParts.push("'Consolas'", 'monospace');

  const fontFamily = fontParts.join(', ');

  root.style.setProperty('--idea-editor-font-family', fontFamily);
  root.style.setProperty('--idea-editor-font-size', `${config.fontSize}px`);
  root.style.setProperty('--idea-editor-line-spacing', String(config.lineSpacing));

  console.log('[Main] Applied IDEA font config:', config, 'fontFamily CSS:', fontFamily);
}

// 注册 applyIdeaFontConfig 函数
window.applyIdeaFontConfig = applyFontConfig;

// 检查是否有待处理的字体配置（Java 端可能先于 JS 执行）
if (window.__pendingFontConfig) {
  console.log('[Main] Found pending font config, applying...');
  applyFontConfig(window.__pendingFontConfig);
  delete window.__pendingFontConfig;
}

/**
 * 应用 IDEA 语言配置到 i18n
 * Only applies IDEA language if user hasn't manually set a language preference
 */
function applyLanguageConfig(config: { language: string; ideaLocale?: string }) {
  const { language } = config;

  // Check if user has manually set a language preference
  const manuallySet = localStorage.getItem('languageManuallySet') === 'true';
  if (manuallySet) {
    console.log('[Main] User has manually set language preference, skipping IDEA language config');
    return;
  }

  // 验证语言代码是否支持
  const supportedLanguages = ['zh', 'en', 'zh-TW', 'hi', 'es', 'fr', 'ja'];
  const targetLanguage = supportedLanguages.includes(language) ? language : 'en';

  console.log('[Main] Applying IDEA language config:', config, 'target language:', targetLanguage);

  // 切换 i18n 语言
  i18n.changeLanguage(targetLanguage)
    .then(() => {
      // 保存到 localStorage，以便下次启动时使用
      localStorage.setItem('language', targetLanguage);
      console.log('[Main] Language changed successfully to:', targetLanguage);
    })
    .catch((error) => {
      console.error('[Main] Failed to change language:', error);
    });
}

// 注册 applyIdeaLanguageConfig 函数
window.applyIdeaLanguageConfig = applyLanguageConfig;

// 检查是否有待处理的语言配置（Java 端可能先于 JS 执行）
if (window.__pendingLanguageConfig) {
  console.log('[Main] Found pending language config, applying...');
  applyLanguageConfig(window.__pendingLanguageConfig);
  delete window.__pendingLanguageConfig;
}

// 预注册 updateStreamingEnabled，避免后端返回状态早于 React 初始化
if (typeof window !== 'undefined' && !window.updateStreamingEnabled) {
  console.log('[Main] Pre-registering updateStreamingEnabled placeholder');
  window.updateStreamingEnabled = (json: string) => {
    console.log('[Main] Storing pending streaming enabled status, length=' + (json ? json.length : 0));
    window.__pendingStreamingEnabled = json;
  };
}

// 预注册 updateSendShortcut，避免后端返回状态早于 React 初始化
if (typeof window !== 'undefined' && !window.updateSendShortcut) {
  console.log('[Main] Pre-registering updateSendShortcut placeholder');
  window.updateSendShortcut = (json: string) => {
    console.log('[Main] Storing pending send shortcut status, length=' + (json ? json.length : 0));
    window.__pendingSendShortcut = json;
  };
}

// 渲染 React 应用
ReactDOM.createRoot(document.getElementById('app') as HTMLElement).render(
  <ErrorBoundary>
    <App />
  </ErrorBoundary>,
);

/**
 * 等待 sendToJava 桥接函数可用
 */
setupScaleRecovery();

function waitForBridge(callback: () => void, maxAttempts = 50, interval = 100) {
  let attempts = 0;

  const check = () => {
    attempts++;
    if (window.sendToJava) {
      console.log('[Main] Bridge available after ' + attempts + ' attempts');
      callback();
    } else if (attempts < maxAttempts) {
      setTimeout(check, interval);
    } else {
      console.error('[Main] Bridge not available after ' + maxAttempts + ' attempts');
    }
  };

  check();
}

// 等待桥接可用后，初始化斜杠命令
waitForBridge(() => {
  startBridgeHeartbeat();

  console.log('[Main] Sending frontend_ready signal');
  sendBridgeEvent('frontend_ready');
});

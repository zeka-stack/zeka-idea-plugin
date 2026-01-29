const BRIDGE_UNAVAILABLE_WARNED = new Set<string>();

/** Path traversal patterns to detect (including URL-encoded variants) */
const PATH_TRAVERSAL_PATTERNS = [
  '..',           // Direct traversal
  '~',            // Home directory reference
  '%2e%2e',       // URL-encoded ..
  '%2E%2E',       // URL-encoded .. (uppercase)
  '%252e%252e',   // Double URL-encoded ..
];

/**
 * Validate file path doesn't contain path traversal patterns
 * Defense-in-depth: backend also validates using canonical paths
 */
const isValidPath = (filePath: string): boolean => {
  if (!filePath) return false;
  // Check both original and decoded path for traversal patterns
  const decodedPath = decodeURIComponent(filePath);
  return !PATH_TRAVERSAL_PATTERNS.some(pattern =>
    filePath.toLowerCase().includes(pattern.toLowerCase()) ||
    decodedPath.toLowerCase().includes(pattern.toLowerCase())
  );
};

const callBridge = (payload: string) => {
  if (window.sendToJava) {
    window.sendToJava(payload);
    return true;
  }
  // Track warned payloads to avoid spam, but don't log to console
  BRIDGE_UNAVAILABLE_WARNED.add(payload);
  return false;
};

export const sendBridgeEvent = (event: string, content = '') => {
  return callBridge(`${event}:${content}`);
};

export const openFile = (filePath?: string) => {
  if (!filePath) {
    return;
  }
  // Security: Validate file path
  if (!isValidPath(filePath)) {
    return;
  }
  sendBridgeEvent('open_file', filePath);
};

export const openBrowser = (url?: string) => {
  if (!url) {
    return;
  }
  sendBridgeEvent('open_browser', url);
};

export const sendToJava = (message: string, payload: any = {}) => {
  const payloadStr = typeof payload === 'string' ? payload : JSON.stringify(payload);
  sendBridgeEvent(message, payloadStr);
};

export const refreshFile = (filePath: string) => {
  if (!filePath) return;
  sendToJava('refresh_file', { filePath });
};

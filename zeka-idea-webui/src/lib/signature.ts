/**
 * HMAC-SHA256 签名工具
 * 用于生成反馈接口的请求签名
 */

// Web UI 的客户端 ID 和 Secret
// 注意：在生产环境中，Secret 应该通过环境变量配置，而不是硬编码
const CLIENT_ID = 'zeka-idea-webui';
const SECRET = import.meta.env.VITE_FEEDBACK_SECRET || '';

/**
 * 生成 UUID v4
 */
function generateUUID(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
}

/**
 * 计算字符串的 SHA256 哈希值（十六进制）
 */
async function sha256Hex(text: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(text);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

/**
 * 计算 HMAC-SHA256 签名并 Base64 编码
 */
async function hmacBase64(secret: string, message: string): Promise<string> {
    const encoder = new TextEncoder();
    const keyData = encoder.encode(secret);
    const messageData = encoder.encode(message);

    const key = await crypto.subtle.importKey(
        'raw',
        keyData,
        {name: 'HMAC', hash: 'SHA-256'},
        false,
        ['sign']
    );

    const signature = await crypto.subtle.sign('HMAC', key, messageData);
    const base64 = btoa(String.fromCharCode(...new Uint8Array(signature)));
    return base64;
}

/**
 * 生成签名请求头
 * @param method HTTP 方法（如 'POST'）
 * @param path 请求路径（如 '/api/plugin/feedback/discussion'）
 * @param body 请求体字符串
 * @returns 包含所有签名头的对象
 */
export async function generateSignatureHeaders(
    method: string,
    path: string,
    body: string
): Promise<Record<string, string>> {
    // 如果没有配置 Secret，返回空对象（不添加签名头）
    if (!SECRET) {
        return {};
    }

    const timestamp = Math.floor(Date.now() / 1000).toString();
    const nonce = generateUUID();
    const bodySha256 = await sha256Hex(body || '');

    // 构建规范字符串
    const canonical = `${method.toUpperCase()}\n${path}\n${bodySha256}\n${timestamp}\n${nonce}`;

    // 计算签名
    const signature = await hmacBase64(SECRET, canonical);

    return {
        'X-Client-Id': CLIENT_ID,
        'X-Timestamp': timestamp,
        'X-Nonce': nonce,
        'X-Body-SHA256': bodySha256,
        'X-Signature': signature,
    };
}

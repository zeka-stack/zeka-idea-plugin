const TOKEN_KEY = 'zeka_auth_token';
const DEVICE_KEY = 'zeka_device_id';

export const authStorage = {
    getToken() {
        return localStorage.getItem(TOKEN_KEY) || '';
    },
    setToken(token: string) {
        if (token) {
            localStorage.setItem(TOKEN_KEY, token);
            window.dispatchEvent(new Event('auth-change'));
        }
    },
    clearToken() {
        localStorage.removeItem(TOKEN_KEY);
        window.dispatchEvent(new Event('auth-change'));
    },
    getDeviceId() {
        return localStorage.getItem(DEVICE_KEY) || '';
    },
    setDeviceId(deviceId: string) {
        if (deviceId) {
            localStorage.setItem(DEVICE_KEY, deviceId);
        }
    },
    clearDeviceId() {
        localStorage.removeItem(DEVICE_KEY);
    }
};

export const authHeaders = () => {
    const token = authStorage.getToken();
    return token ? {Authorization: `Bearer ${token}`} : ({} as Record<string, string>);
};

export const parseTokenFromLocation = () => {
    const search = window.location.search;
    const hash = window.location.hash;
    const searchParams = new URLSearchParams(search);
    const tokenFromSearch = searchParams.get('token');
    if (tokenFromSearch) {
        return tokenFromSearch;
    }
    const hashIndex = hash.indexOf('?');
    if (hashIndex >= 0) {
        const hashQuery = hash.slice(hashIndex + 1);
        const hashParams = new URLSearchParams(hashQuery);
        return hashParams.get('token') || '';
    }
    return '';
};

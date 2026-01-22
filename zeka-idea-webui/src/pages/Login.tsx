import {useEffect, useState} from 'react';
import {Github, KeyRound, ShieldCheck, Sparkles} from 'lucide-react';
import {authHeaders, authStorage, parseTokenFromLocation} from '../lib/auth';

const generateDeviceId = () => {
    if (crypto?.randomUUID) {
        return crypto.randomUUID();
    }
    return `${Math.random().toString(36).slice(2, 10)}-${Date.now().toString(36)}`;
};

export const Login = () => {
    const [deviceId, setDeviceId] = useState(authStorage.getDeviceId());
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [token, setToken] = useState('');

    useEffect(() => {
        const syncToken = () => {
            const next = parseTokenFromLocation();
            if (next && next !== token) {
                setToken(next);
            }
        };
        syncToken();
        window.addEventListener('hashchange', syncToken);
        return () => window.removeEventListener('hashchange', syncToken);
    }, [token]);

    useEffect(() => {
        const search = window.location.search;
        const hash = window.location.hash;
        const searchParams = new URLSearchParams(search);
        const errorParam = searchParams.get('error');
        if (errorParam) {
            setError('GitHub 登录失败，请重试');
        }
        const hashIndex = hash.indexOf('?');
        if (!errorParam && hashIndex >= 0) {
            const hashParams = new URLSearchParams(hash.slice(hashIndex + 1));
            if (hashParams.get('error')) {
                setError('GitHub 登录失败，请重试');
            }
        }
        if (!token) {
            return;
        }
        authStorage.setToken(token);
        setLoading(true);
        fetch('/api/auth/me', {headers: authHeaders()})
            .then((res) => res.json())
            .then(() => {
                window.location.hash = '#/settings';
            })
            .catch(() => {
                setError('登录状态校验失败，请重试');
                authStorage.clearToken();
            })
            .finally(() => {
                setLoading(false);
            });
    }, [token]);

    const handleLogin = async () => {
        if (!deviceId.trim()) {
            setError('请先填写 deviceId');
            return;
        }
        setError('');
        setLoading(true);
        try {
            authStorage.setDeviceId(deviceId.trim());
            const response = await fetch(`/api/oauth/github/login?deviceId=${encodeURIComponent(deviceId.trim())}`);
            const json = await response.json();
            const data = json?.data ?? json;
            if (!data?.url) {
                throw new Error('无法获取登录地址');
            }
            window.location.href = data.url;
        } catch {
            setError('获取 GitHub 登录地址失败');
        } finally {
            setLoading(false);
        }
    };

    const handleGenerate = () => {
        const next = generateDeviceId();
        setDeviceId(next);
        authStorage.setDeviceId(next);
    };

    return (
        <div className="bg-[#F7F5F2] font-['Space_Grotesk'] text-slate-900">
            <div className="relative overflow-hidden">
                <div className="absolute -top-40 -left-32 h-[420px] w-[420px] rounded-full bg-gradient-to-br from-amber-200 via-rose-200 to-indigo-200 blur-3xl opacity-70"/>
                <div className="absolute -bottom-40 -right-32 h-[420px] w-[420px] rounded-full bg-gradient-to-br from-indigo-200 via-teal-200 to-emerald-200 blur-3xl opacity-70"/>
            </div>
            <div className="relative mx-auto max-w-6xl px-6 py-16 lg:py-24">
                <div className="grid gap-10 lg:grid-cols-[1.1fr,0.9fr]">
                    <div className="space-y-8">
                        <div className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm font-medium text-slate-600 shadow-sm">
                            <Sparkles className="h-4 w-4 text-amber-500"/>
                            Zeka Stack · GitHub 认证
                        </div>
                        <h1 className="text-4xl font-semibold leading-tight text-slate-900 md:text-5xl">
                            <span className="block">登录你的 AI 工作台</span>
                            <span className="mt-3 block text-amber-600">绑定设备，开启专属体验</span>
                        </h1>
                        <p className="max-w-xl text-lg text-slate-600">
                            我们使用 GitHub OAuth 进行登录，安全、可靠。绑定 deviceId 后，你的配置将持续可用。
                        </p>
                        <div className="grid gap-4 sm:grid-cols-3">
                            <div className="rounded-2xl border border-slate-200 bg-white/80 p-4 shadow-sm">
                                <ShieldCheck className="h-5 w-5 text-emerald-500"/>
                                <p className="mt-3 text-sm font-medium text-slate-700">授权即用</p>
                                <p className="mt-1 text-xs text-slate-500">只读权限，隐私安全。</p>
                            </div>
                            <div className="rounded-2xl border border-slate-200 bg-white/80 p-4 shadow-sm">
                                <KeyRound className="h-5 w-5 text-indigo-500"/>
                                <p className="mt-3 text-sm font-medium text-slate-700">设备绑定</p>
                                <p className="mt-1 text-xs text-slate-500">独立 deviceId 唯一标识。</p>
                            </div>
                            <div className="rounded-2xl border border-slate-200 bg-white/80 p-4 shadow-sm">
                                <Github className="h-5 w-5 text-slate-700"/>
                                <p className="mt-3 text-sm font-medium text-slate-700">GitHub 登录</p>
                                <p className="mt-1 text-xs text-slate-500">快速、安全的认证。</p>
                            </div>
                        </div>
                    </div>

                    <div className="rounded-[32px] border border-slate-200 bg-white/80 p-8 shadow-xl backdrop-blur">
                        <div className="space-y-6">
                            <div>
                                <h2 className="text-2xl font-semibold text-slate-900">绑定设备并登录</h2>
                                <p className="mt-2 text-sm text-slate-500">deviceId 将与 GitHub 账号唯一绑定。</p>
                            </div>
                            <div>
                                <label className="text-sm font-medium text-slate-700">deviceId</label>
                                <input
                                    value={deviceId}
                                    onChange={(e) => setDeviceId(e.target.value)}
                                    placeholder="例如 dev-xxxx"
                                    className="mt-2 w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 outline-none transition focus:border-slate-400"
                                />
                                <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
                                    <span>可在设置页更换 deviceId。</span>
                                    <button
                                        type="button"
                                        onClick={handleGenerate}
                                        className="rounded-full border border-slate-200 px-3 py-1 text-slate-600 hover:border-slate-300 hover:text-slate-800"
                                    >
                                        生成 deviceId
                                    </button>
                                </div>
                            </div>
                            {error && (
                                <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                                    {error}
                                </div>
                            )}
                            <button
                                type="button"
                                onClick={handleLogin}
                                disabled={loading}
                                className="flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white shadow-lg transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
                            >
                                <Github className="h-4 w-4"/>
                                {loading ? '正在跳转...' : '使用 GitHub 登录'}
                            </button>
                            <p className="text-center text-xs text-slate-400">
                                登录即表示你同意我们的
                                <a href="#/privacy" className="mx-1 text-amber-600 hover:text-amber-500">
                                    隐私政策
                                </a>
                                。
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

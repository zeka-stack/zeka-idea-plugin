import React, {useEffect, useRef, useState} from 'react';
import {AlertCircle, CheckCircle2, ChevronDown, Info} from 'lucide-react';

const plugins = [
    {name: 'IntelliAI Engine', icon: '/icons/engine.svg'},
    {name: 'IntelliAI Javadoc', icon: '/icons/javadoc.svg'},
    {name: 'IntelliAI Changelog', icon: '/icons/changelog.svg'},
    {name: 'IntelliAI Tracer', icon: '/icons/tracer.svg'},
    {name: 'IntelliAI Swagger', icon: '/icons/swagger.svg'},
];

const feedbackTypes = [
    {label: 'Bug 报告', emoji: '🐛', value: 'BUG'},
    {label: '功能建议', emoji: '✨', value: 'FEATURE'},
    {label: '使用问题', emoji: '❓', value: 'QUESTION'},
    {label: '其他', emoji: '📝', value: 'OTHER'},
];

export const Donate: React.FC = () => {
    const [formData, setFormData] = useState({
        type: 'BUG',
        title: '',
        content: '',
        githubUsername: '',
        pluginName: '',
        pluginVersion: '',
        ideaVersion: '',
        os: ''
    });
    const [status, setStatus] = useState<{ type: 'idle' | 'loading' | 'success' | 'error', message: string }>({type: 'idle', message: ''});
    const [showSuccessCard, setShowSuccessCard] = useState(false);
    const [discussionUrl, setDiscussionUrl] = useState<string | null>(null);

    const [isPluginDropdownOpen, setIsPluginDropdownOpen] = useState(false);
    const pluginDropdownRef = useRef<HTMLDivElement>(null);

    const [isTypeDropdownOpen, setIsTypeDropdownOpen] = useState(false);
    const typeDropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (pluginDropdownRef.current && !pluginDropdownRef.current.contains(event.target as Node)) {
                setIsPluginDropdownOpen(false);
            }
            if (typeDropdownRef.current && !typeDropdownRef.current.contains(event.target as Node)) {
                setIsTypeDropdownOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const {name, value} = e.target;
        setFormData(prev => ({...prev, [name]: value}));
    };

    const handlePluginSelect = (name: string) => {
        setFormData(prev => ({...prev, pluginName: name}));
        setIsPluginDropdownOpen(false);
    };

    const handleTypeSelect = (value: string) => {
        setFormData(prev => ({...prev, type: value}));
        setIsTypeDropdownOpen(false);
    };

    const currentType = feedbackTypes.find(t => t.value === formData.type) || feedbackTypes[0];

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!formData.title.trim() || !formData.content.trim()) {
            setStatus({type: 'error', message: '请输入必填字段 (标题和内容)'});
            return;
        }

        setStatus({type: 'loading', message: '正在提交反馈，请稍候...'});

        const requestBody = {
            title: formData.title,
            content: formData.content,
            type: formData.type,
            category: 'GENERAL',
            userInfo: {
                githubUsername: formData.githubUsername || undefined,
                pluginName: formData.pluginName || undefined,
                pluginVersion: formData.pluginVersion || undefined,
                ideaVersion: formData.ideaVersion || undefined,
                os: formData.os || undefined
            },
            metadata: {
                timestamp: Date.now()
            }
        };

        try {
            const body = JSON.stringify(requestBody);
            const signatureHeaders = await import('../lib/signature').then(m =>
                m.generateSignatureHeaders('POST', '/api/plugin/feedback/discussion', body)
            );

            const response = await fetch('/api/plugin/feedback/discussion', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...signatureHeaders
                },
                body
            });

            const responseData = await response.json();
            const requestSuccess = response.ok && responseData.success === true;
            const data = responseData.data;

            if (requestSuccess && data && data.success === true) {
                setStatus({type: 'success', message: data.message || '反馈提交成功！感谢您的反馈。'});
                setFormData({
                    type: 'BUG',
                    title: '',
                    content: '',
                    githubUsername: '',
                    pluginName: '',
                    pluginVersion: '',
                    ideaVersion: '',
                    os: ''
                });

                if (data.discussion && data.discussion.url) {
                    setDiscussionUrl(data.discussion.url);
                } else {
                    setDiscussionUrl(null);
                }
                setShowSuccessCard(true);
            } else {
                const errorMsg = data?.error || data?.message || responseData.message || '提交失败，请稍后重试。';
                setStatus({type: 'error', message: errorMsg});
            }
        } catch (error) {
            const message = error instanceof Error ? error.message : '网络错误';
            setStatus({type: 'error', message: '提交失败：' + message});
        }
    };

    return (
        <div className="min-h-screen py-10 px-4 text-[#212529] font-sans" style={{
            background: 'linear-gradient(120deg, #ff9a62, #ff6f61, #ffc371, #ff9f68)',
            backgroundSize: '300% 300%',
            animation: 'warmGradient 16s ease infinite'
        }}>
            <style>{`
                @keyframes warmGradient {
                    0% { background-position: 0% 50%; }
                    50% { background-position: 100% 50%; }
                    100% { background-position: 0% 50%; }
                }
            `}</style>

            <div className="max-w-[900px] mx-auto">
                {/* Donation Card */}
                <div className="bg-[rgba(255,248,240,0.94)] rounded-xl p-8 mb-8 shadow-[0_10px_30px_rgba(165,84,40,0.2)]">
                    <h2 className="text-2xl font-bold mb-5 pb-2 border-b-2 border-[#4a63d4] text-[#212529]">捐赠方式</h2>

                    <div className="flex flex-col md:flex-row gap-8 justify-center mb-8">
                        <div className="text-center flex-1 min-w-[200px]">
                            <h3 className="text-xl mb-4 font-semibold">微信捐赠</h3>
                            <div className="w-[200px] h-[200px] mx-auto p-2 bg-white border-2 border-[#dee2e6] rounded-lg shadow-sm hover:scale-105 transition-transform duration-300">
                                <img src="https://cdn.dong4j.site/source/image/wechat-pay.webp" alt="微信支付二维码" className="w-full h-full object-contain"/>
                            </div>
                        </div>
                        <div className="text-center flex-1 min-w-[200px]">
                            <h3 className="text-xl mb-4 font-semibold">支付宝捐赠</h3>
                            <div className="w-[200px] h-[200px] mx-auto p-2 bg-white border-2 border-[#dee2e6] rounded-lg shadow-sm hover:scale-105 transition-transform duration-300">
                                <img src="https://cdn.dong4j.site/source/image/alipay.webp" alt="支付宝二维码" className="w-full h-full object-contain"/>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Feedback Card */}
                <div className="bg-[rgba(255,248,240,0.94)] rounded-xl p-8 mb-8 shadow-[0_10px_30px_rgba(165,84,40,0.2)]">
                    <h2 className="text-2xl font-bold mb-5 pb-2 border-b-2 border-[#4a63d4] text-[#212529]">意见反馈</h2>
                    {showSuccessCard && (
                        <div className="mb-6 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
                            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                                <div>
                                    <p className="font-semibold">反馈已提交</p>
                                    <p className="text-xs text-emerald-700/80">感谢你的支持，我们会尽快查看。</p>
                                </div>
                                <div className="flex flex-wrap gap-2">
                                    {discussionUrl && (
                                        <button
                                            type="button"
                                            onClick={() => window.open(discussionUrl, '_blank')}
                                            className="rounded-full bg-emerald-600 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-500"
                                        >
                                            前往查看
                                        </button>
                                    )}
                                    <button
                                        type="button"
                                        onClick={() => setShowSuccessCard(false)}
                                        className="rounded-full border border-emerald-200 bg-white px-4 py-2 text-xs font-semibold text-emerald-700 hover:border-emerald-300"
                                    >
                                        知道了
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Status Alerts */}
                    {status.message && (
                        <div className={`p-4 rounded-md mb-5 flex items-start gap-2 ${
                            status.type === 'error' ? 'bg-[#f8d7da] border border-[#f5c6cb] text-[#721c24]' :
                                status.type === 'success' ? 'bg-[#d4edda] border border-[#c3e6cb] text-[#155724]' :
                                    'bg-[#d1ecf1] border border-[#bee5eb] text-[#0c5460]'
                        }`}>
                            {status.type === 'error' && <AlertCircle className="w-5 h-5 shrink-0"/>}
                            {status.type === 'success' && <CheckCircle2 className="w-5 h-5 shrink-0"/>}
                            {status.type === 'loading' && <Info className="w-5 h-5 shrink-0 animate-pulse"/>}
                            <span>{status.message}</span>
                        </div>
                    )}

                    <form onSubmit={handleSubmit}>
                        <div className="grid grid-cols-1 md:grid-cols-4 gap-5 mb-5">
                            {/* Custom Type Select */}
                            <div className="relative" ref={typeDropdownRef}>
                                <label className="block mb-1 text-sm font-semibold">
                                    反馈类型 <span className="text-[#dc3545]">*</span>
                                </label>
                                <div
                                    className="relative cursor-pointer"
                                    onClick={() => setIsTypeDropdownOpen(!isTypeDropdownOpen)}
                                >
                                    <div className="w-full px-3 py-2 pr-9 border border-[#dee2e6] rounded-md bg-white flex items-center gap-2 text-sm transition-colors focus-within:border-[#4a63d4]">
                                        <span>{currentType.emoji}</span>
                                        <span className="text-gray-700">{currentType.label}</span>
                                    </div>
                                    <ChevronDown className={`absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none transition-transform duration-200 ${isTypeDropdownOpen ? 'rotate-180' : ''}`}/>
                                </div>

                                {isTypeDropdownOpen && (
                                    <div className="absolute z-20 w-full mt-1 bg-white border border-gray-200 rounded-md shadow-lg py-1">
                                        {feedbackTypes.map((type) => (
                                            <div
                                                key={type.value}
                                                onClick={() => handleTypeSelect(type.value)}
                                                className={`flex items-center gap-3 px-3 py-2.5 hover:bg-gray-50 cursor-pointer ${formData.type === type.value ? 'bg-indigo-50 text-indigo-700' : 'text-gray-700'}`}
                                            >
                                                <span>{type.emoji}</span>
                                                <span className="text-sm font-medium">{type.label}</span>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div className="md:col-span-3">
                                <label className="block mb-1 text-sm font-semibold">
                                    标题 <span className="text-[#dc3545]">*</span>
                                </label>
                                <input
                                    type="text"
                                    name="title"
                                    value={formData.title}
                                    onChange={handleInputChange}
                                    required
                                    maxLength={255}
                                    placeholder="请输入反馈标题"
                                    className="w-full px-3 py-2 text-sm border border-[#dee2e6] rounded-md focus:outline-none focus:border-[#4a63d4] transition-colors"
                                />
                            </div>
                        </div>

                        <div className="mb-5">
                            <label className="block mb-1 text-sm font-semibold">
                                内容 <span className="text-[#dc3545]">*</span>
                            </label>
                            <textarea
                                name="content"
                                value={formData.content}
                                onChange={handleInputChange}
                                required
                                maxLength={10000}
                                placeholder="请详细描述您的反馈内容..."
                                className="w-full px-3 py-2 text-sm border border-[#dee2e6] rounded-md min-h-[96px] focus:outline-none focus:border-[#4a63d4] transition-colors"
                            />
                            <small className="block mt-1 text-[#6c757d]">最多 10000 个字符</small>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-5 mb-5">
                            <div>
                                <label className="block mb-1 text-sm font-semibold">GitHub 用户名</label>
                                <input
                                    type="text"
                                    name="githubUsername"
                                    value={formData.githubUsername}
                                    onChange={handleInputChange}
                                    placeholder="用于 @ 提及"
                                    className="w-full px-3 py-2 text-sm border border-[#dee2e6] rounded-md focus:outline-none focus:border-[#4a63d4] transition-colors"
                                />
                            </div>

                            {/* Custom Plugin Select */}
                            <div className="relative" ref={pluginDropdownRef}>
                                <label className="block mb-1 text-sm font-semibold">插件名称</label>
                                <div className="relative">
                                    <input
                                        type="text"
                                        name="pluginName"
                                        value={formData.pluginName}
                                        onChange={handleInputChange}
                                        onFocus={() => setIsPluginDropdownOpen(true)}
                                        placeholder="例如：IntelliAI Javadoc"
                                        className="w-full px-3 py-2 pr-9 text-sm border border-[#dee2e6] rounded-md focus:outline-none focus:border-[#4a63d4] transition-colors"
                                        autoComplete="off"
                                    />
                                    <ChevronDown className={`absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none transition-transform duration-200 ${isPluginDropdownOpen ? 'rotate-180' : ''}`}/>
                                </div>

                                {isPluginDropdownOpen && (
                                    <div className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-md shadow-lg max-h-60 overflow-y-auto overflow-x-hidden">
                                        {plugins.map((plugin) => (
                                            <div
                                                key={plugin.name}
                                                onClick={() => handlePluginSelect(plugin.name)}
                                                className="flex items-center gap-3 px-3 py-2.5 hover:bg-gray-50 cursor-pointer border-b border-gray-50 last:border-0"
                                            >
                                                <div className="w-6 h-6 flex items-center justify-center bg-gray-50 rounded p-0.5">
                                                    <img src={plugin.icon} alt={plugin.name} className="w-full h-full object-contain"/>
                                                </div>
                                                <span className="text-sm font-medium text-gray-700">{plugin.name}</span>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div>
                                <label className="block mb-1 text-sm font-semibold">插件版本</label>
                                <input
                                    type="text"
                                    name="pluginVersion"
                                    value={formData.pluginVersion}
                                    onChange={handleInputChange}
                                    placeholder="例如：1.0.0"
                                    className="w-full px-3 py-2 text-sm border border-[#dee2e6] rounded-md focus:outline-none focus:border-[#4a63d4] transition-colors"
                                />
                            </div>
                            <div>
                                <label className="block mb-1 text-sm font-semibold">IDEA 版本</label>
                                <input
                                    type="text"
                                    name="ideaVersion"
                                    value={formData.ideaVersion}
                                    onChange={handleInputChange}
                                    placeholder="例如：2024.1"
                                    className="w-full px-3 py-2 text-sm border border-[#dee2e6] rounded-md focus:outline-none focus:border-[#4a63d4] transition-colors"
                                />
                            </div>
                            <div className="md:col-span-2">
                                <label className="block mb-1 text-sm font-semibold">操作系统</label>
                                <input
                                    type="text"
                                    name="os"
                                    value={formData.os}
                                    onChange={handleInputChange}
                                    placeholder="例如：macOS 14.0"
                                    className="w-full px-3 py-2 text-sm border border-[#dee2e6] rounded-md focus:outline-none focus:border-[#4a63d4] transition-colors"
                                />
                            </div>
                        </div>

                        <div className="flex justify-end gap-4 mt-8">
                            <button
                                type="button"
                                onClick={() => setFormData(prev => ({...prev, title: '', content: ''}))}
                                className="px-8 py-3 bg-[#6c757d] text-white rounded-md font-bold hover:bg-[#5a6268] transition-colors"
                            >
                                清空
                            </button>
                            <button
                                type="submit"
                                disabled={status.type === 'loading'}
                                className="px-8 py-3 bg-[#4a63d4] text-white rounded-md font-bold hover:bg-[#5a73e4] transition-colors disabled:opacity-70 disabled:cursor-not-allowed shadow-md hover:shadow-lg hover:-translate-y-px"
                            >
                                {status.type === 'loading' ? '提交中...' : '提交反馈'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

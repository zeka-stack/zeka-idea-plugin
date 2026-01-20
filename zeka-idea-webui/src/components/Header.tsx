import React, {useEffect, useState} from 'react';
import {ChevronDown, Sparkles} from 'lucide-react';
import {authHeaders, authStorage} from '../lib/auth';

export const Header: React.FC = () => {
    const [isProductsOpen, setIsProductsOpen] = useState(false);
    const [loggedIn, setLoggedIn] = useState(false);
    const [user, setUser] = useState<{ avatarUrl?: string; githubLogin?: string } | null>(null);

    const refreshAuth = async () => {
        const token = authStorage.getToken();
        if (!token) {
            setLoggedIn(false);
            return;
        }
        try {
            const response = await fetch('/api/auth/me', {headers: authHeaders()});
            const json = await response.json();
            const data = json?.data ?? json;
            const isLoggedIn = Boolean(data?.loggedIn);
            setLoggedIn(isLoggedIn);
            setUser(isLoggedIn ? data.user : null);
        } catch (e) {
            setLoggedIn(false);
            setUser(null);
        }
    };

    useEffect(() => {
        refreshAuth();
        const handle = () => refreshAuth();
        window.addEventListener('auth-change', handle);
        window.addEventListener('hashchange', handle);
        return () => {
            window.removeEventListener('auth-change', handle);
            window.removeEventListener('hashchange', handle);
        };
    }, []);

    return (
        <header className="border-b border-gray-100 bg-white/80 backdrop-blur-md sticky top-0 z-20">
            <div className="max-w-[1400px] mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
                <a href="#/" className="flex items-center gap-2.5 hover:opacity-80 transition-opacity">
                    <div className="w-8 h-8 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-lg flex items-center justify-center shadow-sm">
                        <Sparkles className="w-5 h-5 text-white"/>
                    </div>
                    <span className="font-bold text-lg tracking-tight text-gray-900">Zeka Stack IDEA Plugin</span>
                </a>
                <nav className="flex items-center gap-6">
                    <div className="hidden md:flex items-center gap-6 text-[15px] font-medium text-gray-600">
                        <a href="#/" className="hover:text-gray-900 transition-colors">Home</a>

                        {/* Products Dropdown */}
                        <div
                            className="relative group"
                            onMouseEnter={() => setIsProductsOpen(true)}
                            onMouseLeave={() => setIsProductsOpen(false)}
                        >
                            <button
                                className="flex items-center gap-1 hover:text-gray-900 transition-colors focus:outline-none"
                                aria-expanded={isProductsOpen}
                            >
                                Products
                                <ChevronDown className={`w-4 h-4 transition-transform duration-200 ${isProductsOpen ? 'rotate-180' : ''}`}/>
                            </button>

                            <div className={`absolute top-full left-1/2 -translate-x-1/2 pt-2 w-56 transition-all duration-200 ${isProductsOpen ? 'opacity-100 visible translate-y-0' : 'opacity-0 invisible -translate-y-2'}`}>
                                <div className="bg-white rounded-xl shadow-xl border border-gray-100 overflow-hidden py-1">
                                    <a href="#/plugins/engine" onClick={() => setIsProductsOpen(false)} className="block px-4 py-3 hover:bg-gray-50 transition-colors">
                                        <div className="font-bold text-gray-900">IntelliAI Engine</div>
                                        <div className="text-xs text-gray-500 mt-0.5">Core AI infrastructure</div>
                                    </a>
                                    <a href="#/plugins/javadoc" onClick={() => setIsProductsOpen(false)} className="block px-4 py-3 hover:bg-gray-50 transition-colors border-t border-gray-50">
                                        <div className="font-bold text-gray-900">IntelliAI Javadoc</div>
                                        <div className="text-xs text-gray-500 mt-0.5">Automated documentation</div>
                                    </a>
                                    <a href="#/plugins/changelog" onClick={() => setIsProductsOpen(false)} className="block px-4 py-3 hover:bg-gray-50 transition-colors border-t border-gray-50">
                                        <div className="font-bold text-gray-900">IntelliAI Changelog</div>
                                        <div className="text-xs text-gray-500 mt-0.5">Smart git reporting</div>
                                    </a>
                                    <a href="#/plugins/terminal" onClick={() => setIsProductsOpen(false)} className="block px-4 py-3 hover:bg-gray-50 transition-colors border-t border-gray-50">
                                        <div className="font-bold text-gray-900">IntelliAI Terminal</div>
                                        <div className="text-xs text-gray-500 mt-0.5">AI Terminal Assistant</div>
                                    </a>
                                </div>
                            </div>
                        </div>

                        <a href="#/feedback" className="hover:text-gray-900 transition-colors">Feedback</a>
                        <a href="#/statistics" className="hover:text-gray-900 transition-colors">Statistics</a>
                        <a href="#/donate" className="hover:text-gray-900 transition-colors">Donate</a>
                        <a href="#/changelog" className="hover:text-gray-900 transition-colors">Changelog</a>
                        <a href="#/privacy" className="hover:text-gray-900 transition-colors">Privacy</a>
                        {!loggedIn && (
                            <a href="#/login" className="hover:text-gray-900 transition-colors">Login</a>
                        )}
                        {loggedIn && (
                            <a href="#/settings" className="hover:text-gray-900 transition-colors">Settings</a>
                        )}
                    </div>
                    {loggedIn && (
                        <a
                            href="#/settings"
                            className="flex items-center gap-2 rounded-full border border-gray-200 bg-white p-1 shadow-sm transition hover:border-gray-300"
                            title={user?.githubLogin || 'Account'}
                        >
                            <div className="h-7 w-7 overflow-hidden rounded-full bg-gray-100">
                                {user?.avatarUrl ? (
                                    <img src={user.avatarUrl} alt="avatar" className="h-full w-full object-cover"/>
                                ) : (
                                    <div className="h-full w-full bg-gradient-to-br from-slate-200 to-slate-100"/>
                                )}
                            </div>
                        </a>
                    )}
                </nav>
            </div>
        </header>
    );
};

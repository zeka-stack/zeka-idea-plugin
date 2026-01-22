import React, {useCallback, useEffect, useState} from 'react';
import {ChevronDown, Sparkles} from 'lucide-react';
import {authHeaders, authStorage} from '../lib/auth';

export type HeaderVariant = 'default' | 'dark' | 'terminal';

interface HeaderProps {
    variant?: HeaderVariant;
}

export const Header: React.FC<HeaderProps> = ({variant = 'default'}) => {
    const [isProductsOpen, setIsProductsOpen] = useState(false);
    const [loggedIn, setLoggedIn] = useState(false);
    const [user, setUser] = useState<{ avatarUrl?: string; githubLogin?: string } | null>(null);

    const refreshAuth = useCallback(async () => {
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
        } catch {
            setLoggedIn(false);
            setUser(null);
        }
    }, []);

    useEffect(() => {
        // Set up event listeners for auth changes
        const handle = () => {
            void refreshAuth();
        };

        window.addEventListener('auth-change', handle);
        window.addEventListener('hashchange', handle);

        // Initial auth check - delay to avoid setState in effect
        const timeoutId = setTimeout(() => {
            void refreshAuth();
        }, 0);

        return () => {
            clearTimeout(timeoutId);
            window.removeEventListener('auth-change', handle);
            window.removeEventListener('hashchange', handle);
        };
    }, [refreshAuth]);

    const getStyles = () => {
        switch (variant) {
            case 'dark':
                return {
                    wrapper: "border-b border-white/10 bg-[#0F172A]/80 backdrop-blur-md",
                    logoText: "text-white",
                    navText: "text-indigo-200/80",
                    navHover: "hover:text-white",
                    dropdown: "bg-[#1E293B] border-white/10",
                    dropdownItem: "hover:bg-white/5 text-gray-200",
                    dropdownItemTitle: "text-gray-200",
                    dropdownItemDesc: "text-gray-400",
                    dropdownDivider: "border-white/5",
                    userBorder: "border-white/10 bg-white/5 hover:border-white/20"
                };
            case 'terminal':
                return {
                    wrapper: "border-b border-gray-800 bg-[#0a0a0a]/90 backdrop-blur-md",
                    logoText: "text-[#33ff00]",
                    navText: "text-gray-400",
                    navHover: "hover:text-[#33ff00]",
                    dropdown: "bg-[#0f0f0f] border-gray-800",
                    dropdownItem: "hover:bg-[#33ff00]/10 text-gray-300 hover:text-[#33ff00]",
                    dropdownItemTitle: "text-gray-300 group-hover:text-[#33ff00]",
                    dropdownItemDesc: "text-gray-500",
                    dropdownDivider: "border-gray-800",
                    userBorder: "border-gray-800 bg-gray-900 hover:border-[#33ff00]"
                };
            default:
                return {
                    wrapper: "border-b border-gray-100 bg-white/80 backdrop-blur-md",
                    logoText: "text-gray-900",
                    navText: "text-gray-600",
                    navHover: "hover:text-gray-900",
                    dropdown: "bg-white border-gray-100",
                    dropdownItem: "hover:bg-gray-50 text-gray-900",
                    dropdownItemTitle: "text-gray-900",
                    dropdownItemDesc: "text-gray-500",
                    dropdownDivider: "border-gray-50",
                    userBorder: "border-gray-200 bg-white hover:border-gray-300"
                };
        }
    };

    const s = getStyles();

    return (
        <header className={`${s.wrapper} sticky top-0 z-20 transition-colors duration-300`}>
            <div className="max-w-[1400px] mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
                <a href="#/" className="flex items-center gap-2.5 hover:opacity-80 transition-opacity">
                    <div className="w-8 h-8 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-lg flex items-center justify-center shadow-sm">
                        <Sparkles className="w-5 h-5 text-white"/>
                    </div>
                    <span className={`font-bold text-lg tracking-tight ${s.logoText} transition-colors`}>Zeka Stack IDEA Plugin</span>
                </a>
                <nav className="flex items-center gap-6">
                    <div className={`hidden md:flex items-center gap-6 text-[15px] font-medium ${s.navText} transition-colors`}>
                        <a href="#/" className={`${s.navHover} transition-colors`}>Home</a>

                        {/* Products Dropdown */}
                        <div
                            className="relative group"
                            onMouseEnter={() => setIsProductsOpen(true)}
                            onMouseLeave={() => setIsProductsOpen(false)}
                        >
                            <button
                                className={`flex items-center gap-1 ${s.navHover} transition-colors focus:outline-none`}
                                aria-expanded={isProductsOpen}
                            >
                                Products
                                <ChevronDown className={`w-4 h-4 transition-transform duration-200 ${isProductsOpen ? 'rotate-180' : ''}`}/>
                            </button>

                            <div className={`absolute top-full left-1/2 -translate-x-1/2 pt-2 w-56 transition-all duration-200 ${isProductsOpen ? 'opacity-100 visible translate-y-0' : 'opacity-0 invisible -translate-y-2'}`}>
                                <div className={`${s.dropdown} rounded-xl shadow-xl border overflow-hidden py-1`}>
                                    <a href="#/plugins/engine" onClick={() => setIsProductsOpen(false)} className={`block px-4 py-3 ${s.dropdownItem} transition-colors group`}>
                                        <div className={`font-bold ${s.dropdownItemTitle} transition-colors`}>IntelliAI Engine</div>
                                        <div className={`text-xs ${s.dropdownItemDesc} mt-0.5`}>Core AI infrastructure</div>
                                    </a>
                                    <a href="#/plugins/javadoc" onClick={() => setIsProductsOpen(false)} className={`block px-4 py-3 ${s.dropdownItem} transition-colors border-t ${s.dropdownDivider} group`}>
                                        <div className={`font-bold ${s.dropdownItemTitle} transition-colors`}>IntelliAI Javadoc</div>
                                        <div className={`text-xs ${s.dropdownItemDesc} mt-0.5`}>Automated documentation</div>
                                    </a>
                                    <a href="#/plugins/changelog" onClick={() => setIsProductsOpen(false)} className={`block px-4 py-3 ${s.dropdownItem} transition-colors border-t ${s.dropdownDivider} group`}>
                                        <div className={`font-bold ${s.dropdownItemTitle} transition-colors`}>IntelliAI Changelog</div>
                                        <div className={`text-xs ${s.dropdownItemDesc} mt-0.5`}>Smart git reporting</div>
                                    </a>
                                    <a href="#/plugins/terminal" onClick={() => setIsProductsOpen(false)} className={`block px-4 py-3 ${s.dropdownItem} transition-colors border-t ${s.dropdownDivider} group`}>
                                        <div className={`font-bold ${s.dropdownItemTitle} transition-colors`}>IntelliAI Terminal</div>
                                        <div className={`text-xs ${s.dropdownItemDesc} mt-0.5`}>AI Terminal Assistant</div>
                                    </a>
                                    <a href="#/plugins/tracer" onClick={() => setIsProductsOpen(false)} className={`block px-4 py-3 ${s.dropdownItem} transition-colors border-t ${s.dropdownDivider} group`}>
                                        <div className={`font-bold ${s.dropdownItemTitle} transition-colors`}>IntelliAI Tracer</div>
                                        <div className={`text-xs ${s.dropdownItemDesc} mt-0.5`}>Code Flow Analysis</div>
                                    </a>
                                    <a href="#/plugins/repairer" onClick={() => setIsProductsOpen(false)} className={`block px-4 py-3 ${s.dropdownItem} transition-colors border-t ${s.dropdownDivider} group`}>
                                        <div className={`font-bold ${s.dropdownItemTitle} transition-colors`}>IntelliAI Repairer</div>
                                        <div className={`text-xs ${s.dropdownItemDesc} mt-0.5`}>Automated Code Remediation</div>
                                    </a>
                                </div>
                            </div>
                        </div>

                        <a href="#/feedback" className={`${s.navHover} transition-colors`}>Feedback</a>
                        <a href="#/statistics" className={`${s.navHover} transition-colors`}>Statistics</a>
                        <a href="#/donate" className={`${s.navHover} transition-colors`}>Donate</a>
                        <a href="#/changelog" className={`${s.navHover} transition-colors`}>Changelog</a>
                        <a href="#/privacy" className={`${s.navHover} transition-colors`}>Privacy</a>
                        {!loggedIn && (
                            <a href="#/login" className={`${s.navHover} transition-colors`}>Login</a>
                        )}
                        {loggedIn && (
                            <a href="#/settings" className={`${s.navHover} transition-colors`}>Settings</a>
                        )}
                    </div>
                    {loggedIn && (
                        <a
                            href="#/settings"
                            className={`flex items-center gap-2 rounded-full border ${s.userBorder} p-1 shadow-sm transition`}
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

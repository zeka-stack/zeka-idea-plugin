import React from 'react';
import {BookOpen, Github, Home} from 'lucide-react';

interface FooterProps {
    variant?: 'default' | 'dark' | 'transparent' | 'transparent-dark';
}

export const Footer: React.FC<FooterProps> = ({variant = 'default'}) => {
    const isDark = variant === 'dark';
    const isTransparent = variant === 'transparent'; // White text on transparent
    const isTransparentDark = variant === 'transparent-dark'; // Dark text on transparent

    // Base container classes
    let containerColorClasses = 'bg-white border-gray-100 text-gray-500'; // default
    if (isDark) {
        containerColorClasses = 'bg-[#0B1120] border-white/10 text-gray-400';
    } else if (isTransparent) {
        containerColorClasses = 'bg-transparent border-transparent text-white/90';
    } else if (isTransparentDark) {
        containerColorClasses = 'bg-transparent border-transparent text-gray-900/90';
    }

    const containerClasses = `w-full py-4 mt-auto border-t transition-colors duration-300 ${containerColorClasses}`;

    // Text classes for meta info
    let textClasses = 'text-gray-600';
    if (isDark || isTransparent) {
        textClasses = 'text-gray-300';
    } else if (isTransparentDark) {
        textClasses = 'text-gray-800';
    }

    // Link hover classes
    let linkHoverClasses = 'hover:text-indigo-600';
    if (isDark || isTransparent) {
        linkHoverClasses = 'hover:text-white';
    } else if (isTransparentDark) {
        linkHoverClasses = 'hover:text-black';
    }

    // Dot classes
    let dotClasses = 'bg-gray-300';
    if (isDark || isTransparent) {
        dotClasses = 'bg-gray-600';
    } else if (isTransparentDark) {
        dotClasses = 'bg-gray-800/40';
    }

    return (
        <footer className={containerClasses}>
            <div className="max-w-[1400px] mx-auto px-4 sm:px-6">
                <div className="flex flex-col md:flex-row items-center justify-between gap-4 text-sm">
                    {/* Meta Info */}
                    <div className={`flex flex-wrap items-center justify-center gap-3 ${textClasses}`}>
                        <span>© 2025 IntelliAI Engine</span>
                        <span className={`w-1 h-1 rounded-full ${dotClasses}`}></span>
                        <span>Powered by zeka.stack</span>
                        <span className={`hidden sm:block w-1 h-1 rounded-full ${dotClasses}`}></span>
                        <span className="hidden sm:inline">Engine powers every AI plugin · Build beyond limits</span>
                    </div>

                    {/* Links */}
                    <div className="flex items-center gap-6">
                        <a
                            href="https://github.com/dong4j"
                            target="_blank"
                            rel="noopener noreferrer"
                            className={`flex items-center gap-2 transition-colors ${linkHoverClasses}`}
                            title="GitHub"
                        >
                            <Github className="w-4 h-4"/>
                            <span>GitHub</span>
                        </a>
                        <a
                            href="https://home.dong4j.site"
                            target="_blank"
                            rel="noopener noreferrer"
                            className={`flex items-center gap-2 transition-colors ${linkHoverClasses}`}
                            title="个人主页"
                        >
                            <Home className="w-4 h-4"/>
                            <span>主页</span>
                        </a>
                        <a
                            href="https://blog.dong4j.site"
                            target="_blank"
                            rel="noopener noreferrer"
                            className={`flex items-center gap-2 transition-colors ${linkHoverClasses}`}
                            title="博客"
                        >
                            <BookOpen className="w-4 h-4"/>
                            <span>博客</span>
                        </a>
                    </div>
                </div>
            </div>
        </footer>
    );
};

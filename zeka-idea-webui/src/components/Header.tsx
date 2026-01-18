import React from 'react';
import {Sparkles} from 'lucide-react';

export const Header: React.FC = () => {
    return (
        <header className="border-b border-gray-100 bg-white/80 backdrop-blur-md sticky top-0 z-20">
            <div className="max-w-[1400px] mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-lg flex items-center justify-center shadow-sm">
                        <Sparkles className="w-5 h-5 text-white"/>
                    </div>
                    <span className="font-bold text-lg tracking-tight text-gray-900">Zeka Stack IDEA Plugin</span>
                </div>
                <nav className="flex items-center gap-6">
                    <div className="hidden md:flex items-center gap-6 text-[15px] font-medium text-gray-600">
                        <a href="#" className="hover:text-gray-900 transition-colors">Features</a>
                        <a href="#" className="hover:text-gray-900 transition-colors">Pricing</a>
                        <a href="#" className="text-indigo-600">Roadmap</a>
                    </div>
                </nav>
            </div>
        </header>
    );
};

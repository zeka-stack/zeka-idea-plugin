import React from 'react';
import {Calendar, CheckSquare, Clock, FileDiff, GitCommit, List} from 'lucide-react';

export const ChangelogHome: React.FC = () => {
    return (
        <div className="min-h-screen bg-[#F0FDF4] font-sans">
            {/* Sidebar Layout */}
            <div className="flex flex-col md:flex-row min-h-screen">

                {/* Left Panel: Hero */}
                <div className="w-full md:w-1/2 p-12 lg:p-20 flex flex-col justify-center bg-[#DCFCE7] border-r border-[#BBF7D0]">
                    <div className="max-w-xl">
                        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-md bg-[#166534] text-white text-xs font-bold uppercase tracking-wider mb-8">
                            <GitCommit className="w-4 h-4"/>
                            IntelliAI Changelog
                        </div>

                        <h1 className="text-4xl lg:text-6xl font-black text-[#14532D] mb-8 leading-tight">
                            Turn Git Commits into <span className="text-[#16A34A]">Stories</span>.
                        </h1>

                        <p className="text-xl text-[#15803D] mb-12 font-medium">
                            Automatically generate structured changelogs, daily reports, and smart commit messages from your git history
                            using AI.
                        </p>

                        <div className="space-y-6">
                            <div className="flex items-start gap-4">
                                <div className="p-3 bg-white rounded-lg shadow-sm">
                                    <Calendar className="w-6 h-6 text-green-600"/>
                                </div>
                                <div>
                                    <h3 className="text-lg font-bold text-[#14532D]">Daily & Weekly Reports</h3>
                                    <p className="text-[#166534]">Summarize your work for stand-ups instantly.</p>
                                </div>
                            </div>

                            <div className="flex items-start gap-4">
                                <div className="p-3 bg-white rounded-lg shadow-sm">
                                    <List className="w-6 h-6 text-green-600"/>
                                </div>
                                <div>
                                    <h3 className="text-lg font-bold text-[#14532D]">Semantic Changelogs</h3>
                                    <p className="text-[#166534]">Group commits by feature, fix, or refactor automatically.</p>
                                </div>
                            </div>

                            <div className="flex items-start gap-4">
                                <div className="p-3 bg-white rounded-lg shadow-sm">
                                    <FileDiff className="w-6 h-6 text-green-600"/>
                                </div>
                                <div>
                                    <h3 className="text-lg font-bold text-[#14532D]">Smart Commit Messages</h3>
                                    <p className="text-[#166534]">Let AI write your commit messages based on diffs.</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right Panel: Interactive / Visuals */}
                <div className="w-full md:w-1/2 bg-[#F0FDF4] p-12 lg:p-20 flex items-center justify-center relative overflow-hidden">
                    {/* Background Pattern */}
                    <div className="absolute inset-0 opacity-10" style={{
                        backgroundImage: 'radial-gradient(#15803D 1px, transparent 1px)',
                        backgroundSize: '24px 24px'
                    }}></div>

                    <div className="relative w-full max-w-lg">
                        {/* Timeline Visual */}
                        <div className="absolute left-8 top-0 bottom-0 w-0.5 bg-green-200"></div>

                        <div className="space-y-12 relative z-10">
                            {/* Item 1: Raw Commits */}
                            <div className="flex gap-8">
                                <div className="w-16 flex-shrink-0 flex flex-col items-center">
                                    <div className="w-4 h-4 rounded-full bg-green-300 border-4 border-white shadow-sm mb-2"></div>
                                    <span className="text-xs text-green-600 font-mono">INPUT</span>
                                </div>
                                <div className="flex-1 bg-white p-5 rounded-lg border border-green-100 shadow-sm opacity-60 scale-95 origin-left">
                                    <div className="font-mono text-xs text-gray-500 space-y-2">
                                        <p>e3f1a2 fix: null pointer in user service</p>
                                        <p>8b2c9d feat: add dark mode support</p>
                                        <p>4a1d5c refactor: optimize database query</p>
                                    </div>
                                </div>
                            </div>

                            {/* Processing Arrow */}
                            <div className="flex justify-center -my-6 pl-16">
                                <div className="p-2 bg-green-500 rounded-full text-white shadow-md animate-bounce">
                                    <Clock className="w-5 h-5"/>
                                </div>
                            </div>

                            {/* Item 2: Generated Report */}
                            <div className="flex gap-8">
                                <div className="w-16 flex-shrink-0 flex flex-col items-center">
                                    <div className="w-4 h-4 rounded-full bg-green-600 border-4 border-white shadow-sm mb-2"></div>
                                    <span className="text-xs text-green-800 font-bold font-mono">OUTPUT</span>
                                </div>
                                <div className="flex-1 bg-white p-6 rounded-xl border border-green-200 shadow-xl transform transition-transform hover:scale-105">
                                    <h3 className="font-bold text-gray-800 mb-4 border-b pb-2">📅 Daily Report</h3>
                                    <div className="space-y-4 text-sm">
                                        <div>
                                            <h4 className="font-semibold text-green-700 flex items-center gap-2">
                                                <CheckSquare className="w-4 h-4"/> Completed Features
                                            </h4>
                                            <p className="text-gray-600 pl-6 mt-1">• Implemented system-wide Dark Mode support.</p>
                                        </div>
                                        <div>
                                            <h4 className="font-semibold text-green-700 flex items-center gap-2">
                                                <CheckSquare className="w-4 h-4"/> Bug Fixes
                                            </h4>
                                            <p className="text-gray-600 pl-6 mt-1">• Resolved NPE in UserService during validation.</p>
                                        </div>
                                        <div>
                                            <h4 className="font-semibold text-green-700 flex items-center gap-2">
                                                <CheckSquare className="w-4 h-4"/> Technical Debt
                                            </h4>
                                            <p className="text-gray-600 pl-6 mt-1">• Optimized slow DB queries for 30% speedup.</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        </div>
    );
};

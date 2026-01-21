import React, {useEffect, useState} from 'react';
import {AlertTriangle, ArrowRight, CheckCircle2, FileCode, Microscope, Play, ShieldCheck, Wrench, Zap} from 'lucide-react';

export const RepairerHome: React.FC = () => {
    const [codeState, setCodeState] = useState<'error' | 'fixing' | 'fixed'>('error');

    useEffect(() => {
        const interval = setInterval(() => {
            setCodeState(current => {
                if (current === 'error') return 'fixing';
                if (current === 'fixing') return 'fixed';
                return 'error'; // Cycle back
            });
        }, 4000); // Change state every 4 seconds

        return () => clearInterval(interval);
    }, []);

    return (
        <div className="min-h-screen bg-[#0f172a] text-slate-300 font-sans selection:bg-teal-500/30">
            {/* Hero Section */}
            <section className="relative pt-24 pb-32 overflow-hidden">
                <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/diagmonds-light.png')] opacity-5 pointer-events-none"></div>
                <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-teal-500/10 rounded-full blur-[120px] pointer-events-none"></div>
                <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-indigo-500/10 rounded-full blur-[100px] pointer-events-none"></div>

                <div className="max-w-7xl mx-auto px-4 sm:px-6 relative z-10 text-center">
                    <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-teal-500/10 border border-teal-500/20 text-teal-400 text-sm font-semibold mb-8 animate-fade-in-up">
                        <Wrench className="w-4 h-4"/>
                        <span>Automated Code Remediation</span>
                    </div>

                    <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight mb-8">
                        <span className="bg-clip-text text-transparent bg-gradient-to-r from-teal-400 via-cyan-400 to-indigo-400">
                            IntelliAI Repairer
                        </span>
                    </h1>

                    <p className="text-xl text-slate-400 max-w-3xl mx-auto mb-12 leading-relaxed font-light">
                        面向 Checkstyle/PMD 违规的智能修复专家。
                        <br/>
                        精准定位，片段替换，让代码质量守护不再繁琐。
                    </p>

                    <div className="flex flex-col sm:flex-row items-center justify-center gap-6">
                        <button className="px-10 py-4 bg-teal-600 hover:bg-teal-500 text-white rounded-lg font-bold transition-all shadow-lg shadow-teal-500/25 flex items-center gap-2 group">
                            安装插件
                            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform"/>
                        </button>
                        <button className="px-10 py-4 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-white rounded-lg font-bold transition-all flex items-center gap-2">
                            查看源码
                        </button>
                    </div>
                </div>
            </section>

            {/* Interactive Demo Section */}
            <section className="py-16 bg-[#0b1120] border-y border-slate-800/50 relative">
                <div className="max-w-5xl mx-auto px-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
                        <div>
                            <h2 className="text-3xl font-bold text-white mb-6 flex items-center gap-3">
                                <Microscope className="text-teal-400"/>
                                智能诊断与修复
                            </h2>
                            <p className="text-slate-400 text-lg mb-8 leading-relaxed">
                                Repairer 不仅仅是发现问题，更是解决问题。它深度集成 Checkstyle-IDEA，当检测到代码规范违规时，直接提供 AI 驱动的
                                QuickFix 选项。
                            </p>
                            <div className="space-y-4">
                                <div className="flex items-start gap-4 p-4 rounded-lg bg-slate-800/50 border border-slate-700/50">
                                    <div className="p-2 bg-red-500/10 rounded-md text-red-400 mt-1">
                                        <AlertTriangle className="w-5 h-5"/>
                                    </div>
                                    <div>
                                        <h3 className="font-bold text-slate-200">违规检测</h3>
                                        <p className="text-sm text-slate-500 mt-1">实时捕获 Checkstyle/PMD 报错。</p>
                                    </div>
                                </div>
                                <div className="flex items-start gap-4 p-4 rounded-lg bg-slate-800/50 border border-slate-700/50">
                                    <div className="p-2 bg-teal-500/10 rounded-md text-teal-400 mt-1">
                                        <Zap className="w-5 h-5"/>
                                    </div>
                                    <div>
                                        <h3 className="font-bold text-slate-200">AI 智能修复</h3>
                                        <p className="text-sm text-slate-500 mt-1">基于上下文生成合规代码，精准替换问题片段。</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Code Animation Window */}
                        <div className="bg-[#1e293b] rounded-xl overflow-hidden shadow-2xl border border-slate-700 relative h-[320px]">
                            <div className="flex items-center justify-between px-4 py-3 bg-[#0f172a] border-b border-slate-700">
                                <div className="flex gap-2">
                                    <div className="w-3 h-3 rounded-full bg-slate-600"></div>
                                    <div className="w-3 h-3 rounded-full bg-slate-600"></div>
                                </div>
                                <span className="text-xs text-slate-500 font-mono">UserService.java</span>
                            </div>

                            <div className="p-6 font-mono text-sm relative">
                                {codeState === 'error' && (
                                    <div className="animate-in fade-in duration-500">
                                        <div className="text-slate-500 mb-2">// TODO: Checkstyle Violation: Line length &gt; 80</div>
                                        <div className="flex items-center">
                                            <span className="text-purple-400">public</span>&nbsp;
                                            <span className="text-yellow-400">void</span>&nbsp;
                                            <span className="text-blue-400">updateUser</span>(...args) {'{'}
                                        </div>
                                        <div className="ml-4 mt-2 p-1 bg-red-500/20 border-b-2 border-red-500 text-slate-300 whitespace-nowrap overflow-hidden text-ellipsis">
                                            String msg = "User " + user.getName() + " has been updated successfully at " + new
                                            Date().toString() + " by system.";
                                        </div>
                                        <div className="ml-4 mt-1 text-red-400 text-xs flex items-center gap-1">
                                            <AlertTriangle className="w-3 h-3"/>
                                            Line is longer than 80 characters (found 110).
                                        </div>
                                    </div>
                                )}

                                {codeState === 'fixing' && (
                                    <div className="flex flex-col items-center justify-center h-full pt-12 animate-in zoom-in duration-300">
                                        <div className="w-12 h-12 border-4 border-teal-500/30 border-t-teal-500 rounded-full animate-spin mb-4"></div>
                                        <p className="text-teal-400 font-bold">AI Repairing...</p>
                                        <p className="text-slate-500 text-xs mt-2">Generating fix strategy...</p>
                                    </div>
                                )}

                                {codeState === 'fixed' && (
                                    <div className="animate-in fade-in duration-500">
                                        <div className="text-slate-500 mb-2">// Fixed by IntelliAI Repairer</div>
                                        <div className="flex items-center">
                                            <span className="text-purple-400">public</span>&nbsp;
                                            <span className="text-yellow-400">void</span>&nbsp;
                                            <span className="text-blue-400">updateUser</span>(...args) {'{'}
                                        </div>
                                        <div className="ml-4 mt-2 text-slate-300">
                                            String msg = String.format(<br/>
                                            &nbsp;&nbsp;&nbsp;&nbsp;"User %s has been updated successfully at %s by system.",<br/>
                                            &nbsp;&nbsp;&nbsp;&nbsp;user.getName(),<br/>
                                            &nbsp;&nbsp;&nbsp;&nbsp;new Date().toString()<br/>
                                            );
                                        </div>
                                        <div className="ml-4 mt-4 flex items-center gap-2 text-teal-400 text-xs font-bold bg-teal-500/10 px-2 py-1 rounded w-fit">
                                            <CheckCircle2 className="w-3 h-3"/>
                                            Checkstyle Passed
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Feature Grid */}
            <section className="py-24 px-4 max-w-7xl mx-auto">
                <div className="text-center mb-16">
                    <h2 className="text-3xl font-bold text-white mb-4">Why IntelliAI Repairer?</h2>
                    <p className="text-slate-500">不仅是修复工具，更是代码质量的守护者</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    <div className="p-8 rounded-2xl bg-[#1e293b]/50 border border-slate-700 hover:border-teal-500/50 transition-all group">
                        <div className="w-12 h-12 rounded-lg bg-teal-500/10 flex items-center justify-center mb-6 group-hover:bg-teal-500/20 transition-colors">
                            <ShieldCheck className="w-6 h-6 text-teal-400"/>
                        </div>
                        <h3 className="text-xl font-bold text-white mb-3">安全可控</h3>
                        <p className="text-slate-400 text-sm leading-relaxed">
                            采用“片段替换”策略，修复范围严格限制在违规代码段内，避免 AI 幻觉修改无关逻辑，确保业务安全。
                        </p>
                    </div>

                    <div className="p-8 rounded-2xl bg-[#1e293b]/50 border border-slate-700 hover:border-indigo-500/50 transition-all group">
                        <div className="w-12 h-12 rounded-lg bg-indigo-500/10 flex items-center justify-center mb-6 group-hover:bg-indigo-500/20 transition-colors">
                            <FileCode className="w-6 h-6 text-indigo-400"/>
                        </div>
                        <h3 className="text-xl font-bold text-white mb-3">无缝集成</h3>
                        <p className="text-slate-400 text-sm leading-relaxed">
                            深度集成 Checkstyle-IDEA 插件，直接在扫描结果中提供 QuickFix 入口，无需切换上下文。
                        </p>
                    </div>

                    <div className="p-8 rounded-2xl bg-[#1e293b]/50 border border-slate-700 hover:border-cyan-500/50 transition-all group">
                        <div className="w-12 h-12 rounded-lg bg-cyan-500/10 flex items-center justify-center mb-6 group-hover:bg-cyan-500/20 transition-colors">
                            <Play className="w-6 h-6 text-cyan-400"/>
                        </div>
                        <h3 className="text-xl font-bold text-white mb-3">未来展望</h3>
                        <p className="text-slate-400 text-sm leading-relaxed">
                            即将支持解析 Maven/Gradle 构建生成的 Checkstyle/PMD XML 报告，并在 Problems 面板实现一键批量修复。
                        </p>
                    </div>
                </div>
            </section>
        </div>
    );
};

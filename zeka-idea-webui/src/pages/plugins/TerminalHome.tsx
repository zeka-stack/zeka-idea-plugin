import React, {useEffect, useState} from 'react';
import {BookOpen, ChevronRight, Command, Cpu, Download, Hash, ShieldCheck, Terminal} from 'lucide-react';

export const TerminalHome: React.FC = () => {
    const [terminalState, setTerminalState] = useState({
        showPromptComment: false,
        typedComment: '',
        showAiAnalysis: false,
        showPromptCommand: false,
        typedCommand: ''
    });

    const commentText = '# 查找所有包含 "TODO" 的 Java 文件';
    const commandText = 'find . -name "*.java" -exec grep -l "TODO" {} \\;';

    useEffect(() => {
        const runSequence = async () => {
            // Reset state
            setTerminalState({
                showPromptComment: true,
                typedComment: '',
                showAiAnalysis: false,
                showPromptCommand: false,
                typedCommand: ''
            });

            // 1. Type Comment
            for (let i = 0; i <= commentText.length; i++) {
                setTerminalState(prev => ({...prev, typedComment: commentText.slice(0, i)}));
                await new Promise(r => setTimeout(r, 50 + Math.random() * 30));
            }

            // Pause before AI analysis
            await new Promise(r => setTimeout(r, 500));

            // 2. Show AI Analysis
            setTerminalState(prev => ({...prev, showAiAnalysis: true}));

            // Simulate analysis time
            await new Promise(r => setTimeout(r, 1500));

            // 3. Show Command Prompt
            setTerminalState(prev => ({...prev, showPromptCommand: true}));

            // 4. Type Command
            for (let i = 0; i <= commandText.length; i++) {
                setTerminalState(prev => ({...prev, typedCommand: commandText.slice(0, i)}));
                await new Promise(r => setTimeout(r, 30 + Math.random() * 20));
            }

            // 5. Wait and restart
            await new Promise(r => setTimeout(r, 5000));
            runSequence();
        };

        runSequence();

        return () => {
            // Cleanup logic if needed (complex with async/await loop, but useEffect cleanup runs on unmount)
            // Ideally we'd use a ref to cancel the loop
        };
    }, []);

    return (
        <div className="min-h-screen bg-[#0a0a0a] text-[#33ff00] font-mono selection:bg-[#33ff00] selection:text-black">
            {/* Hero Section */}
            <section className="pt-32 pb-20 px-4 relative overflow-hidden">
                {/* Background Grid */}
                <div className="absolute inset-0 bg-[linear-gradient(to_right,#1f2937_1px,transparent_1px),linear-gradient(to_bottom,#1f2937_1px,transparent_1px)] bg-[size:4rem_4rem] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)] opacity-20 pointer-events-none"></div>

                <div className="max-w-6xl mx-auto relative z-10">
                    <div className="mb-8 inline-flex items-center gap-2 border border-[#33ff00] px-4 py-1.5 text-sm bg-[#33ff00]/5 rounded-sm">
                        <span className="relative flex h-2 w-2">
                          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#33ff00] opacity-75"></span>
                          <span className="relative inline-flex rounded-full h-2 w-2 bg-[#33ff00]"></span>
                        </span>
                        <span className="tracking-widest">SYSTEM ONLINE</span>
                    </div>

                    <h1 className="text-5xl md:text-7xl font-bold tracking-tighter mb-8 leading-tight">
                        <span className="text-white">IntelliAI</span>
                        <span className="text-[#33ff00]">_Terminal</span>
                        <span className="animate-pulse text-[#33ff00]">_</span>
                    </h1>

                    <div className="text-xl md:text-2xl text-gray-400 max-w-3xl mb-12 space-y-2">
                        <p className="flex items-center gap-3">
                            <span className="text-[#33ff00]">$</span>
                            <span>将自然语言瞬间转化为 Shell 命令。</span>
                        </p>
                        <p className="flex items-center gap-3">
                            <span className="text-[#33ff00]">$</span>
                            <span>AI 驱动，深度集成于 IDE 终端。</span>
                        </p>
                        <p className="flex items-center gap-3">
                            <span className="text-[#33ff00]">$</span>
                            <span>只需输入描述并按 <span className="text-white bg-slate-800 border-b-4 border-slate-900 px-3 py-1 rounded text-base font-bold mx-1 shadow-lg">TAB</span> 即可生成。</span>
                        </p>
                    </div>

                    <div className="flex flex-col sm:flex-row gap-6">
                        <button className="group relative px-8 py-4 bg-[#33ff00] text-black font-bold text-lg hover:bg-[#2adb00] transition-all">
                            <div className="absolute inset-0 border-2 border-[#33ff00] translate-x-1.5 translate-y-1.5 group-hover:translate-x-2.5 group-hover:translate-y-2.5 transition-transform bg-transparent content-[''] -z-10 border-dashed"></div>
                            <span className="flex items-center gap-2">
                                <Download className="w-5 h-5"/>
                                安装插件
                            </span>
                        </button>

                        <button className="px-8 py-4 bg-transparent border border-gray-700 text-gray-300 font-bold text-lg hover:border-[#33ff00] hover:text-[#33ff00] transition-all flex items-center gap-2 group">
                            <BookOpen className="w-5 h-5 group-hover:scale-110 transition-transform"/>
                            查看文档
                        </button>
                    </div>
                </div>
            </section>

            {/* Terminal Demo */}
            <section className="py-12 px-4 bg-[#050505] border-y border-gray-900 relative">
                <div className="absolute top-0 left-0 w-full h-px bg-gradient-to-r from-transparent via-[#33ff00]/50 to-transparent"></div>
                <div className="max-w-4xl mx-auto">
                    <div className="bg-[#0c0c0c] rounded-lg overflow-hidden shadow-2xl border border-gray-800 font-mono text-sm md:text-base relative group">
                        {/* Terminal Glow */}
                        <div className="absolute -inset-1 bg-gradient-to-r from-[#33ff00] to-emerald-600 rounded-lg blur opacity-5 group-hover:opacity-10 transition duration-1000 group-hover:duration-200"></div>

                        {/* Terminal Bar */}
                        <div className="bg-[#1a1a1a] px-4 py-2 flex items-center gap-2 border-b border-gray-800 relative z-10">
                            <div className="flex gap-2">
                                <div className="w-2.5 h-2.5 rounded-full bg-[#ff5f56]"></div>
                                <div className="w-2.5 h-2.5 rounded-full bg-[#ffbd2e]"></div>
                                <div className="w-2.5 h-2.5 rounded-full bg-[#27c93f]"></div>
                            </div>
                            <div className="ml-4 text-gray-500 text-[10px] flex-1 text-center font-mono tracking-tight">user@dev-machine:
                                ~/projects/zeka-stack
                            </div>
                        </div>

                        {/* Terminal Content */}
                        <div className="p-5 text-gray-300 min-h-[160px] relative z-10 flex flex-col justify-center">
                            {/* Interaction */}
                            <div className="mb-2 h-6 flex items-center">
                                {terminalState.showPromptComment && (
                                    <>
                                        <span className="text-[#33ff00] mr-2">➜</span>
                                        <span className="text-[#39a0ed] mr-2">~</span>
                                        <span className="text-gray-500 italic">{terminalState.typedComment}</span>
                                        {!terminalState.showAiAnalysis && (
                                            <span className="animate-pulse bg-gray-500 w-2 h-4 inline-block ml-1 align-middle"></span>
                                        )}
                                    </>
                                )}
                            </div>

                            <div className="mb-4 h-8 flex items-center">
                                {terminalState.showAiAnalysis && (
                                    <div className="pl-4 border-l-2 border-[#33ff00]/30 text-xs text-gray-500 animate-in fade-in slide-in-from-left-2 duration-300">
                                        <span className="text-[#33ff00]">[AI]</span> 正在分析请求...
                                    </div>
                                )}
                            </div>

                            <div className="mb-4 h-6 flex items-center">
                                {terminalState.showPromptCommand && (
                                    <>
                                        <span className="text-[#33ff00] mr-2">➜</span>
                                        <span className="text-[#39a0ed] mr-2">~</span>
                                        <span className="text-white">{terminalState.typedCommand}</span>
                                        <span className="animate-pulse bg-[#33ff00] w-2.5 h-5 inline-block ml-1 align-middle shadow-[0_0_8px_rgba(51,255,0,0.8)]"></span>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
                <div className="absolute bottom-0 left-0 w-full h-px bg-gradient-to-r from-transparent via-[#33ff00]/50 to-transparent"></div>
            </section>

            {/* Features Grid */}
            <section className="py-24 px-4">
                <div className="max-w-6xl mx-auto">
                    <div className="flex items-center gap-4 mb-16">
                        <h2 className="text-3xl font-bold text-white">
                            <span className="text-[#33ff00]">./</span>核心功能
                        </h2>
                        <div className="h-px bg-gray-800 flex-1"></div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {[
                            {icon: Command, title: '命令生成', desc: '自然语言直接转 Shell 命令，消除记忆负担。'},
                            {icon: Hash, title: '智能提取', desc: '自动剥离提示符（$, >），精准提取用户意图。'},
                            {icon: ShieldCheck, title: '安全校验', desc: '内置语法校验，确保生成的命令安全可执行。'},
                            {icon: Cpu, title: '引擎驱动', desc: '基于 IntelliAI Engine，支持 OpenAI、Ollama 等多模型。'},
                            {icon: Terminal, title: '双模支持', desc: '完美适配 TerminalView 和 JBTerminalWidget。'},
                            {icon: ChevronRight, title: '灵活配置', desc: '自定义触发前缀与 Prompt 模板，适配个人习惯。'},
                        ].map((feature, idx) => (
                            <div key={idx} className="p-6 border border-gray-800 bg-[#0f0f0f] hover:border-[#33ff00] hover:bg-[#33ff00]/5 transition-all group relative overflow-hidden">
                                <div className="absolute top-0 right-0 p-3 opacity-10 group-hover:opacity-20 transition-opacity">
                                    <feature.icon className="w-24 h-24 text-[#33ff00]"/>
                                </div>
                                <feature.icon className="w-8 h-8 text-gray-500 group-hover:text-[#33ff00] mb-6 transition-colors"/>
                                <h3 className="text-lg font-bold text-gray-200 group-hover:text-white mb-2 font-mono tracking-wide">{feature.title}</h3>
                                <p className="text-gray-500 text-sm leading-relaxed group-hover:text-gray-400">
                                    {feature.desc}
                                </p>
                            </div>
                        ))}
                    </div>
                </div>
            </section>
        </div>
    );
};

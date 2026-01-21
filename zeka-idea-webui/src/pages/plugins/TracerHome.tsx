import React from 'react';
import {Activity, ArrowRight, Cpu, FileText, GitBranch, Layout, Search, ShieldCheck, Zap} from 'lucide-react';

export const TracerHome: React.FC = () => {
    return (
        <div className="min-h-screen bg-[#020617] text-slate-300 font-sans selection:bg-cyan-500/30">
            {/* Hero Section */}
            <section className="relative pt-24 pb-32 overflow-hidden">
                {/* Decorative background elements */}
                <div className="absolute top-0 left-0 w-full h-full bg-[url('https://www.transparenttextures.com/patterns/carbon-fibre.png')] opacity-10 pointer-events-none"></div>
                <div className="absolute -top-24 -left-24 w-96 h-96 bg-blue-600/20 rounded-full blur-[120px]"></div>
                <div className="absolute top-1/2 -right-24 w-80 h-80 bg-cyan-500/10 rounded-full blur-[100px]"></div>

                <div className="max-w-7xl mx-auto px-4 sm:px-6 relative z-10 text-center">
                    <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-sm font-semibold mb-8 animate-pulse">
                        <Activity className="w-4 h-4"/>
                        <span>Next-Gen Code Flow Analysis</span>
                    </div>

                    <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight mb-8">
                        <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-400 via-cyan-300 to-indigo-400">
                            IntelliAI Tracer
                        </span>
                    </h1>

                    <p className="text-xl text-slate-400 max-w-3xl mx-auto mb-12 leading-relaxed font-light">
                        深度追踪代码执行流。自动生成调用链、时序图及业务逻辑文档，
                        让复杂的系统架构与业务逻辑一目了然。
                    </p>

                    <div className="flex flex-col sm:flex-row items-center justify-center gap-6">
                        <button className="px-10 py-4 bg-blue-600 hover:bg-blue-500 text-white rounded-lg font-bold transition-all shadow-lg shadow-blue-500/25 flex items-center gap-2 group">
                            安装插件
                            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform"/>
                        </button>
                        <button className="px-10 py-4 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-white rounded-lg font-bold transition-all flex items-center gap-2">
                            开发指南
                        </button>
                    </div>
                </div>
            </section>

            {/* Diagnostic Visualization Section */}
            <section className="py-20 bg-[#030816] border-y border-slate-800/50 relative overflow-hidden">
                <div className="max-w-6xl mx-auto px-4 sm:px-6">
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
                        <div className="space-y-8">
                            <div className="space-y-4">
                                <h2 className="text-3xl font-bold text-white flex items-center gap-3">
                                    <Search className="text-cyan-400"/>
                                    智能代码扫描与分析
                                </h2>
                                <p className="text-slate-400 leading-relaxed">
                                    不再迷失在繁琐的方法调用中。Tracer 自动提取类依赖、方法上下文及注解信息，构建完整的技术全景。
                                </p>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                                <div className="p-5 rounded-xl bg-slate-900/50 border border-slate-800 hover:border-blue-500/50 transition-colors group">
                                    <GitBranch className="w-8 h-8 text-blue-400 mb-3 group-hover:scale-110 transition-transform"/>
                                    <h3 className="font-bold text-slate-100 mb-1">调用链追踪</h3>
                                    <p className="text-sm text-slate-500">自动识别方法的上下游调用，支持间接与递归调用分析。</p>
                                </div>
                                <div className="p-5 rounded-xl bg-slate-900/50 border border-slate-800 hover:border-cyan-500/50 transition-colors group">
                                    <Layout className="w-8 h-8 text-cyan-400 mb-3 group-hover:scale-110 transition-transform"/>
                                    <h3 className="font-bold text-slate-100 mb-1">类关系建模</h3>
                                    <p className="text-sm text-slate-500">可视化类的继承与组合关系，清晰展示依赖注入结构。</p>
                                </div>
                            </div>
                        </div>

                        {/* Animated Code Flow Representation */}
                        <div className="relative">
                            <div className="absolute -inset-1 bg-gradient-to-r from-blue-500 to-cyan-500 rounded-2xl blur opacity-20"></div>
                            <div className="relative bg-[#0f172a] rounded-xl border border-slate-700 overflow-hidden shadow-2xl">
                                <div className="flex items-center gap-2 px-4 py-3 bg-[#1e293b] border-b border-slate-700">
                                    <div className="w-3 h-3 rounded-full bg-slate-600"></div>
                                    <span className="text-xs text-slate-400 font-mono italic">CallGraph_Inspector.v1</span>
                                </div>
                                <div className="p-6 font-mono text-xs sm:text-sm space-y-4">
                                    <div className="flex items-start gap-3">
                                        <div className="w-1.5 h-1.5 rounded-full bg-blue-500 mt-1.5"></div>
                                        <div>
                                            <span className="text-blue-400">UserService</span>.createUser()
                                            <div className="pl-4 mt-2 border-l border-slate-700 space-y-2">
                                                <div className="text-slate-500">├── validateInput()</div>
                                                <div className="text-slate-500">├── <span className="text-cyan-400">UserMapper</span>.insertUser()
                                                </div>
                                                <div className="text-slate-500">└── <span className="text-indigo-400">AuditLogger</span>.log()
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div className="p-3 bg-slate-800/50 rounded border border-slate-700 text-slate-400 italic">
                                        <span className="text-cyan-400 font-bold">[AI Analysis]</span>: 该流程采用 Service
                                        模式封装，包含了参数验证、持久化及异步审计记录。建议对 log 调用进行异常隔离。
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Core Capabilities */}
            <section className="py-24 max-w-7xl mx-auto px-4 sm:px-6">
                <div className="text-center mb-16">
                    <h2 className="text-3xl font-bold text-white mb-4">核心修复与分析能力</h2>
                    <p className="text-slate-500 max-w-2xl mx-auto">结合 AI 的代码修复与流分析，助力高效理解和重构复杂业务系统。</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    <div className="p-8 rounded-2xl bg-[#0f172a] border border-slate-800 hover:bg-slate-900/50 transition-all">
                        <div className="w-12 h-12 rounded-lg bg-indigo-500/10 flex items-center justify-center mb-6">
                            <Zap className="w-6 h-6 text-indigo-400"/>
                        </div>
                        <h3 className="text-xl font-bold text-white mb-3">AI 解释工作流</h3>
                        <p className="text-slate-400 text-sm leading-relaxed">
                            Alt+Enter 瞬间唤醒 AI。理解代码背后的设计意图，自动生成技术规格说明，消除阅读遗留代码的痛苦。
                        </p>
                    </div>

                    <div className="p-8 rounded-2xl bg-[#0f172a] border border-slate-800 hover:bg-slate-900/50 transition-all">
                        <div className="w-12 h-12 rounded-lg bg-emerald-500/10 flex items-center justify-center mb-6">
                            <FileText className="w-6 h-6 text-emerald-400"/>
                        </div>
                        <h3 className="text-xl font-bold text-white mb-3">时序图自动生成</h3>
                        <p className="text-slate-400 text-sm leading-relaxed">
                            将复杂的代码逻辑转化为直观的 UML 时序图。支持交互式浏览，清晰呈现多组件间的交互时序。
                        </p>
                    </div>

                    <div className="p-8 rounded-2xl bg-[#0f172a] border border-slate-800 hover:bg-slate-900/50 transition-all">
                        <div className="w-12 h-12 rounded-lg bg-cyan-500/10 flex items-center justify-center mb-6">
                            <ShieldCheck className="w-6 h-6 text-cyan-400"/>
                        </div>
                        <h3 className="text-xl font-bold text-white mb-3">架构优化建议</h3>
                        <p className="text-slate-400 text-sm leading-relaxed">
                            AI 深度评估代码质量与设计模式，自动识别循环依赖和高耦合模块，并提供具体的架构重构建议。
                        </p>
                    </div>
                </div>
            </section>

            {/* AI Engine Support */}
            <section className="py-20 text-center border-t border-slate-800/50">
                <div className="max-w-4xl mx-auto px-4">
                    <Cpu className="w-16 h-16 text-blue-500/50 mx-auto mb-6"/>
                    <h2 className="text-2xl font-bold text-white mb-4">由 IntelliAI Engine 提供动力</h2>
                    <p className="text-slate-400 mb-8">
                        无缝集成多种 AI 提供商。无论是本地运行的 Ollama 还是强大的 GPT-4 / 通义千问，都能为您提供最精准的代码分析结果。
                    </p>
                    <div className="flex justify-center gap-8 opacity-50 grayscale hover:grayscale-0 transition-all">
                        {/* Placeholder logos for AI providers */}
                        <span className="font-bold text-slate-300">OpenAI</span>
                        <span className="font-bold text-slate-300">Ollama</span>
                        <span className="font-bold text-slate-300">Aliyun</span>
                        <span className="font-bold text-slate-300">Grok</span>
                    </div>
                </div>
            </section>
        </div>
    );
};

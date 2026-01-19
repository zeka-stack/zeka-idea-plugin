import React from 'react';
import {Activity, Cpu, Layers, Lock, Plug, Server, Share2} from 'lucide-react';

export const EngineHome: React.FC = () => {
    return (
        <div className="min-h-screen bg-[#0F172A] text-white font-sans selection:bg-cyan-500/30">
            {/* Hero Section */}
            <section className="relative pt-20 pb-32 overflow-hidden">
                <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20"></div>
                <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[500px] bg-indigo-500/20 rounded-full blur-[100px]"></div>

                <div className="max-w-7xl mx-auto px-4 sm:px-6 relative z-10 text-center">
                    <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 text-sm font-medium mb-8">
                        <Activity className="w-4 h-4"/>
                        <span>Core Infrastructure for Zeka Stack</span>
                    </div>

                    <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight mb-8 bg-clip-text text-transparent bg-gradient-to-b from-white to-indigo-200">
                        IntelliAI Engine
                    </h1>

                    <p className="text-xl text-indigo-200/80 max-w-3xl mx-auto mb-12 leading-relaxed">
                        The unified AI service orchestration layer. Manage providers, secure credentials, and standardize AI capabilities
                        across all your IntelliJ plugins.
                    </p>

                    <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
                        <button className="px-8 py-4 bg-indigo-600 hover:bg-indigo-500 rounded-lg font-bold transition-all shadow-lg shadow-indigo-500/25 flex items-center gap-2">
                            <Plug className="w-5 h-5"/>
                            Integrate Engine
                        </button>
                        <button className="px-8 py-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-lg font-bold transition-all flex items-center gap-2">
                            <Share2 className="w-5 h-5"/>
                            View API Docs
                        </button>
                    </div>
                </div>
            </section>

            {/* Architecture Diagram */}
            <section className="py-20 bg-[#0B1120] border-y border-white/5">
                <div className="max-w-6xl mx-auto px-4 sm:px-6">
                    <div className="text-center mb-16">
                        <h2 className="text-3xl font-bold mb-4">Unified Architecture</h2>
                        <p className="text-indigo-300/60">Decoupling business logic from AI providers</p>
                    </div>

                    <div className="relative p-8 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-sm">
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-center">
                            {/* Clients */}
                            <div className="space-y-4">
                                <div className="p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-emerald-300 font-mono text-sm text-center">
                                    IntelliAI Javadoc
                                </div>
                                <div className="p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-emerald-300 font-mono text-sm text-center">
                                    IntelliAI Changelog
                                </div>
                                <div className="p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-emerald-300 font-mono text-sm text-center">
                                    Custom Plugins
                                </div>
                            </div>

                            {/* Engine Core */}
                            <div className="relative">
                                <div className="absolute inset-0 bg-indigo-500/20 blur-xl"></div>
                                <div className="relative p-8 bg-indigo-900/40 border border-indigo-500/30 rounded-xl text-center">
                                    <Cpu className="w-12 h-12 text-indigo-400 mx-auto mb-4"/>
                                    <h3 className="text-xl font-bold text-white mb-2">IntelliAI Engine</h3>
                                    <div className="text-xs text-indigo-300 space-y-1">
                                        <p>• Config Management</p>
                                        <p>• Credential Vault</p>
                                        <p>• Error Handling</p>
                                    </div>
                                </div>
                                {/* Arrows */}
                                <div className="hidden md:block absolute top-1/2 -left-8 w-8 h-0.5 bg-indigo-500/30"></div>
                                <div className="hidden md:block absolute top-1/2 -right-8 w-8 h-0.5 bg-indigo-500/30"></div>
                            </div>

                            {/* Providers */}
                            <div className="space-y-4">
                                <div className="p-4 bg-blue-500/10 border border-blue-500/20 rounded-lg text-blue-300 font-mono text-sm text-center">
                                    OpenAI
                                </div>
                                <div className="p-4 bg-blue-500/10 border border-blue-500/20 rounded-lg text-blue-300 font-mono text-sm text-center">
                                    Aliyun QianWen
                                </div>
                                <div className="p-4 bg-blue-500/10 border border-blue-500/20 rounded-lg text-blue-300 font-mono text-sm text-center">
                                    Ollama (Local)
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Features */}
            <section className="py-24">
                <div className="max-w-7xl mx-auto px-4 sm:px-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
                        <div className="p-6 rounded-2xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
                            <Layers className="w-10 h-10 text-indigo-400 mb-4"/>
                            <h3 className="text-lg font-bold mb-2">Code Reuse</h3>
                            <p className="text-sm text-gray-400">Share AI logic across multiple plugins without duplication.</p>
                        </div>
                        <div className="p-6 rounded-2xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
                            <Server className="w-10 h-10 text-purple-400 mb-4"/>
                            <h3 className="text-lg font-bold mb-2">Unified Config</h3>
                            <p className="text-sm text-gray-400">One-time configuration for all AI services. Switch providers instantly.</p>
                        </div>
                        <div className="p-6 rounded-2xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
                            <Lock className="w-10 h-10 text-emerald-400 mb-4"/>
                            <h3 className="text-lg font-bold mb-2">Secure Vault</h3>
                            <p className="text-sm text-gray-400">API keys are stored safely using IntelliJ Password Safe.</p>
                        </div>
                        <div className="p-6 rounded-2xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
                            <Activity className="w-10 h-10 text-orange-400 mb-4"/>
                            <h3 className="text-lg font-bold mb-2">Easy Integration</h3>
                            <p className="text-sm text-gray-400">Simple `AIService.call(prompt)` API for 3rd party plugins.</p>
                        </div>
                    </div>
                </div>
            </section>
        </div>
    );
};

import React from 'react';
import {BookOpen, Cloud, Settings2, Sparkles, Terminal} from 'lucide-react';

export const JavadocHome: React.FC = () => {
    return (
        <div className="min-h-screen bg-[#FFFBF0] text-gray-800 font-serif">
            {/* Header / Hero */}
            <header className="border-b-4 border-orange-500 bg-[#FFFAF0] py-20 px-4">
                <div className="max-w-5xl mx-auto text-center">
                    <div className="inline-block p-4 rounded-full bg-orange-100 mb-6 border-2 border-orange-200">
                        <BookOpen className="w-10 h-10 text-orange-600"/>
                    </div>
                    <h1 className="text-5xl md:text-6xl font-black text-gray-900 mb-6 tracking-tight">
                        IntelliAI <span className="text-orange-600 underline decoration-4 underline-offset-4">Javadoc</span>
                    </h1>
                    <p className="text-xl md:text-2xl text-gray-600 max-w-2xl mx-auto mb-10 font-sans">
                        AI-powered documentation generator. Turn your code into clear, standardized Javadoc with a single click.
                    </p>
                    <div className="flex flex-col sm:flex-row items-center justify-center gap-4 font-sans">
                        <button className="px-8 py-3 bg-gray-900 text-white rounded-md font-bold hover:bg-gray-800 transition-all shadow-[4px_4px_0px_0px_rgba(0,0,0,0.2)] active:translate-y-1 active:shadow-none border-2 border-gray-900">
                            Install Plugin
                        </button>
                        <button className="px-8 py-3 bg-white text-gray-900 rounded-md font-bold hover:bg-gray-50 transition-all shadow-[4px_4px_0px_0px_rgba(234,88,12,0.2)] active:translate-y-1 active:shadow-none border-2 border-gray-200">
                            Read Guide
                        </button>
                    </div>
                </div>
            </header>

            {/* Feature Strip */}
            <div className="bg-orange-600 text-white py-4 font-sans font-bold text-center tracking-wider overflow-hidden">
                <div className="flex justify-center gap-12 opacity-90">
                    <span>★ QIANWEN SUPPORT</span>
                    <span>★ OLLAMA LOCAL</span>
                    <span>★ JUNIT INTEGRATION</span>
                    <span>★ CONTEXT AWARE</span>
                </div>
            </div>

            {/* Main Content */}
            <main className="max-w-6xl mx-auto px-4 py-20 font-sans">
                {/* Demo Section */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 mb-24 items-center">
                    <div>
                        <h2 className="text-3xl font-bold mb-6 flex items-center gap-3">
                            <Sparkles className="text-orange-500 fill-orange-500"/>
                            Smart Generation
                        </h2>
                        <p className="text-lg text-gray-600 mb-6 leading-relaxed">
                            Stop writing boilerplate documentation manually. IntelliAI Javadoc analyzes your method signatures, parameter
                            names, and logic to generate comprehensive documentation.
                        </p>
                        <ul className="space-y-4">
                            {[
                                'Auto-detects method context (Class, Field, Method)',
                                'Supports custom templates for standardization',
                                'Recognizes JUnit 4/5 tests automatically',
                                'Incremental updates - preserves existing docs'
                            ].map((item, i) => (
                                <li key={i} className="flex items-center gap-3 text-gray-700 font-medium">
                                    <div className="w-2 h-2 bg-orange-500 rounded-full"></div>
                                    {item}
                                </li>
                            ))}
                        </ul>
                    </div>

                    <div className="bg-white p-2 rounded-xl border-2 border-gray-200 shadow-[8px_8px_0px_0px_rgba(0,0,0,0.05)]">
                        <div className="bg-[#1E1E1E] rounded-lg p-6 font-mono text-sm text-gray-300 overflow-x-auto">
                            <div className="text-gray-500 mb-2">/**</div>
                            <div className="text-gray-500 mb-2">&nbsp;* Calculating total order price with tax.</div>
                            <div className="text-gray-500 mb-2">&nbsp;*</div>
                            <div className="text-gray-500 mb-2">&nbsp;* @param items List of order items</div>
                            <div className="text-gray-500 mb-2">&nbsp;* @param taxRate Current regional tax rate</div>
                            <div className="text-gray-500 mb-2">&nbsp;* @return Total price formatted as BigDecimal</div>
                            <div className="text-gray-500 mb-2">&nbsp;*/</div>
                            <div><span className="text-purple-400">public</span> <span className="text-yellow-400">BigDecimal</span>
                                <span className="text-blue-400">calculateTotal</span>(List&lt;Item&gt; items, <span className="text-orange-400">double</span> taxRate) {'{'}
                            </div>
                            <div className="ml-4 text-gray-500">// ...</div>
                            <div>{'}'}</div>
                        </div>
                        <div className="text-center mt-4 mb-2">
              <span className="bg-green-100 text-green-800 text-xs font-bold px-3 py-1 rounded-full border border-green-200">
                Generated in 1.2s
              </span>
                        </div>
                    </div>
                </div>

                {/* Providers Grid */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    <div className="bg-white p-8 rounded-xl border-2 border-gray-100 hover:border-orange-200 transition-colors">
                        <Cloud className="w-8 h-8 text-blue-500 mb-4"/>
                        <h3 className="text-xl font-bold mb-3">QianWen (Aliyun)</h3>
                        <p className="text-gray-500 text-sm">
                            Powerful cloud-based model with excellent Chinese language support. Ideal for enterprise environments.
                        </p>
                    </div>
                    <div className="bg-white p-8 rounded-xl border-2 border-gray-100 hover:border-orange-200 transition-colors">
                        <Terminal className="w-8 h-8 text-gray-800 mb-4"/>
                        <h3 className="text-xl font-bold mb-3">Ollama Local</h3>
                        <p className="text-gray-500 text-sm">
                            Run open-source models (Llama 3, Qwen) locally on your machine. 100% private, no API keys needed.
                        </p>
                    </div>
                    <div className="bg-white p-8 rounded-xl border-2 border-gray-100 hover:border-orange-200 transition-colors">
                        <Settings2 className="w-8 h-8 text-purple-500 mb-4"/>
                        <h3 className="text-xl font-bold mb-3">Custom / OpenAI</h3>
                        <p className="text-gray-500 text-sm">
                            Connect to any OpenAI-compatible endpoint. Use GPT-4, Azure OpenAI, or self-hosted services.
                        </p>
                    </div>
                </div>
            </main>
        </div>
    );
};

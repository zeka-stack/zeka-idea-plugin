import React from 'react';
import {Database, Eye, Lock, Server, Shield} from 'lucide-react';

export const PrivacyPolicy: React.FC = () => {
    return (
        <div className="min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8 font-sans text-gray-700">
            <div className="max-w-3xl mx-auto">
                <div className="text-center mb-12">
                    <div className="flex justify-center mb-4">
                        <div className="p-3 bg-indigo-100 rounded-full">
                            <Shield className="w-8 h-8 text-indigo-600"/>
                        </div>
                    </div>
                    <h1 className="text-3xl font-extrabold text-gray-900 sm:text-4xl">
                        隐私政策
                    </h1>
                    <p className="mt-4 text-lg text-gray-500">
                        Zeka Engine 插件如何收集、使用和保护您的信息
                    </p>
                </div>

                <div className="space-y-10">
                    <section>
                        <h2 className="flex items-center text-xl font-bold text-gray-900 mb-4">
                            <Eye className="w-5 h-5 mr-2 text-indigo-500"/>
                            1. 我们收集的信息
                        </h2>
                        <div className="bg-gray-50 rounded-xl p-6 border border-gray-100">
                            <p className="mb-4">
                                为了提供更好的编码辅助服务，Zeka Engine 插件可能会收集以下类型的信息：
                            </p>
                            <ul className="space-y-3 list-disc list-inside text-gray-600">
                                <li>
                                    <span className="font-semibold text-gray-800">代码片段与上下文：</span>
                                    为了进行代码补全、分析和生成，我们需要临时处理您编辑器中的代码片段。
                                </li>
                                <li>
                                    <span className="font-semibold text-gray-800">使用统计数据：</span>
                                    包括功能调用频率、响应时间、错误日志等，用于优化插件性能。
                                </li>
                                <li>
                                    <span className="font-semibold text-gray-800">设备与环境信息：</span>
                                    IDE 版本、操作系统类型、插件版本等基础环境信息。
                                </li>
                            </ul>
                        </div>
                    </section>

                    <section>
                        <h2 className="flex items-center text-xl font-bold text-gray-900 mb-4">
                            <Database className="w-5 h-5 mr-2 text-indigo-500"/>
                            2. 信息的用途
                        </h2>
                        <p className="mb-4 text-gray-600">
                            我们收集的信息仅用于以下目的：
                        </p>
                        <ul className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <li className="flex items-start">
                                <div className="flex-shrink-0 w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 mr-2"></div>
                                <span>提供智能代码补全和重构建议</span>
                            </li>
                            <li className="flex items-start">
                                <div className="flex-shrink-0 w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 mr-2"></div>
                                <span>诊断崩溃和性能问题</span>
                            </li>
                            <li className="flex items-start">
                                <div className="flex-shrink-0 w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 mr-2"></div>
                                <span>改进 AI 模型的准确性</span>
                            </li>
                            <li className="flex items-start">
                                <div className="flex-shrink-0 w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 mr-2"></div>
                                <span>验证许可证有效性</span>
                            </li>
                        </ul>
                    </section>

                    <section>
                        <h2 className="flex items-center text-xl font-bold text-gray-900 mb-4">
                            <Lock className="w-5 h-5 mr-2 text-indigo-500"/>
                            3. 数据保护与安全
                        </h2>
                        <div className="prose prose-indigo text-gray-600">
                            <p>
                                我们要特别声明：<strong className="text-gray-900">您的代码是您的私有资产。</strong>
                            </p>
                            <p className="mt-2">
                                除非您明确选择加入我们的数据贡献计划（如适用），否则所有的代码处理均在本地或经由加密通道传输至我们的安全服务器进行短暂处理后立即丢弃，<span className="text-indigo-600 font-medium">我们不会持久化存储您的业务代码</span>。
                            </p>
                        </div>
                    </section>

                    <section>
                        <h2 className="flex items-center text-xl font-bold text-gray-900 mb-4">
                            <Server className="w-5 h-5 mr-2 text-indigo-500"/>
                            4. 第三方服务
                        </h2>
                        <p className="text-gray-600">
                            本插件可能集成第三方大语言模型（LLM）服务。在使用这些服务时，相关数据处理将遵循第三方服务商的隐私政策。我们会尽最大努力筛选合规且注重隐私的合作伙伴。
                        </p>
                    </section>

                    <div className="border-t border-gray-200 pt-8 mt-10">
                        <p className="text-sm text-gray-400 text-center">
                            最后更新日期：{new Date().toLocaleDateString()}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

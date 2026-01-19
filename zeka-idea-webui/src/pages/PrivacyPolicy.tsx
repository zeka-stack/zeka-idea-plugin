import React from 'react';
import {AlertTriangle, Database, Eye, Lock, Server, Shield} from 'lucide-react';

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
                        隐私政策与免责声明
                    </h1>
                    <p className="mt-4 text-lg text-gray-500">
                        Zeka Engine 插件如何收集、使用和保护您的信息
                    </p>
                </div>

                <div className="space-y-12">
                    {/* 1. 收集的信息 */}
                    <section>
                        <h2 className="flex items-center text-xl font-bold text-gray-900 mb-4">
                            <Eye className="w-5 h-5 mr-2 text-indigo-500"/>
                            1. 我们收集的信息
                        </h2>
                        <div className="bg-gray-50 rounded-xl p-6 border border-gray-100">
                            <p className="mb-4 text-gray-700">
                                为了提供更好的编码辅助服务及持续优化产品体验，Zeka Engine 插件可能会收集以下类型的信息：
                            </p>

                            <div className="space-y-6">
                                <div>
                                    <h3 className="font-bold text-gray-800 mb-2">A. 代码片段与上下文（仅临时处理）</h3>
                                    <p className="text-sm text-gray-600 leading-relaxed">
                                        为了进行代码补全、分析、生成和重构，我们需要临时读取您编辑器中当前活跃文件的代码片段及相关上下文（如光标位置、选中内容）。
                                        <br/>
                                        <span className="text-indigo-600 font-medium">注意：这些数据仅用于实时处理，处理完成后会立即从内存中丢弃，不会被持久化存储到我们的服务器。</span>
                                    </p>
                                </div>

                                <div>
                                    <h3 className="font-bold text-gray-800 mb-2">B. 使用统计数据（匿名化）</h3>
                                    <p className="text-sm text-gray-600 mb-2">
                                        我们仅收集与插件功能相关的匿名统计数据，用于优化性能与功能体验，包含以下类型的信息：
                                    </p>
                                    <ul className="list-disc list-inside text-sm text-gray-600 space-y-1 pl-2">
                                        <li><span className="font-medium text-gray-700">功能使用情况：</span> 事件类型（如 Javadoc/Commit
                                            Message/Changelog 等）与使用频次。
                                        </li>
                                        <li><span className="font-medium text-gray-700">性能指标：</span> 单次请求耗时、成功/失败状态。</li>
                                        <li><span className="font-medium text-gray-700">模型信息：</span> AI 服务商与模型名称（仅名称，不含任何凭据）。
                                        </li>
                                        <li><span className="font-medium text-gray-700">消耗统计：</span> Token 消耗概况（输入/输出/总量）。
                                        </li>
                                        <li><span className="font-medium text-gray-700">触发入口：</span> 项目树/编辑器/快捷键/提交面板/Git
                                            日志面板等入口类型。
                                        </li>
                                        <li><span className="font-medium text-gray-700">时间与维度：</span> 事件时间、项目名称（仅用于统计维度，不包含代码）。
                                        </li>
                                        <li><span className="font-medium text-gray-700">匿名设备标识：</span> 本地生成的匿名标识，仅用于统计去重与跨设备一致性。
                                        </li>
                                    </ul>
                                    <p className="mt-3 text-sm text-gray-600">
                                        <span className="text-indigo-600 font-medium">我们不会收集或存储您的 API Key、Access Token、Secret 等任何凭据。</span>
                                    </p>
                                </div>

                                <div>
                                    <h3 className="font-bold text-gray-800 mb-2">C. 明确不收集的信息</h3>
                                    <ul className="list-disc list-inside text-sm text-gray-600 space-y-1 pl-2">
                                        <li>用户的 API Key、密钥、Token、密码或任何敏感凭据</li>
                                        <li>代码文件内容、项目源代码、提交 diff 或文件路径</li>
                                        <li>与本隐私政策描述无关的任何数据</li>
                                    </ul>
                                    <p className="mt-2 text-sm text-gray-600">
                                        我们<strong>不会</strong>收集本段描述之外的任何数据。
                                    </p>
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* 2. 信息的用途 */}
                    <section>
                        <h2 className="flex items-center text-xl font-bold text-gray-900 mb-4">
                            <Database className="w-5 h-5 mr-2 text-indigo-500"/>
                            2. 信息的用途
                        </h2>
                        <p className="mb-4 text-gray-600">
                            我们收集的数据仅用于统计页面的数据展示与聚合分析，不用于广告投放、用户画像或任何个性化营销。
                        </p>
                        <ul className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <li className="flex items-start">
                                <div className="flex-shrink-0 w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 mr-2"></div>
                                <span>展示功能使用量与事件类型分布</span>
                            </li>
                            <li className="flex items-start">
                                <div className="flex-shrink-0 w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 mr-2"></div>
                                <span>展示性能与耗时等统计指标</span>
                            </li>
                            <li className="flex items-start">
                                <div className="flex-shrink-0 w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 mr-2"></div>
                                <span>展示模型与服务商使用占比</span>
                            </li>
                            <li className="flex items-start">
                                <div className="flex-shrink-0 w-1.5 h-1.5 rounded-full bg-indigo-500 mt-2 mr-2"></div>
                                <span>展示入口来源分布与趋势变化</span>
                            </li>
                        </ul>
                    </section>

                    {/* 3. 数据保护 */}
                    <section>
                        <h2 className="flex items-center text-xl font-bold text-gray-900 mb-4">
                            <Lock className="w-5 h-5 mr-2 text-indigo-500"/>
                            3. 数据保护与安全
                        </h2>
                        <div className="prose prose-indigo text-gray-600 text-sm">
                            <p className="mb-3">
                                我们将您的数据安全视为重中之重。
                            </p>
                            <ul className="list-disc list-inside space-y-2">
                                <li><strong>加密传输：</strong> 所有数据传输均采用 HTTPS/TLS 加密协议。</li>
                                <li><strong>本地处理优先：</strong> 我们尽可能将逻辑放在本地运行。对于必须依赖云端 AI 的功能，我们仅发送最小必要上下文。
                                </li>
                                <li><strong>敏感信息存储：</strong> 您的 API Key 等敏感凭证存储在 IDE 本地的安全存储区（IntelliJ Password
                                    Safe），我们无法直接读取，更不会上传。
                                </li>
                                <li><strong>不留存代码：</strong> 除非您明确参与特定的数据贡献计划，否则我们绝对不会在服务器上持久化存储您的任何业务代码。
                                </li>
                            </ul>
                        </div>
                    </section>

                    {/* 4. 第三方服务 */}
                    <section>
                        <h2 className="flex items-center text-xl font-bold text-gray-900 mb-4">
                            <Server className="w-5 h-5 mr-2 text-indigo-500"/>
                            4. 第三方服务
                        </h2>
                        <p className="text-gray-600 text-sm leading-relaxed">
                            本插件集成了多家第三方大语言模型（LLM）服务商（如 OpenAI, Aliyun Qianwen, Anthropic 等）以提供 AI 能力。
                            当您配置并使用这些服务时，您的请求数据（包含代码片段）将直接发送至相应的服务商。
                            <br/><br/>
                            这些数据的处理将遵循各第三方服务商的隐私政策。我们强烈建议您在使用前查阅相关服务商的隐私条款。我们会尽最大努力筛选合规且注重隐私的合作伙伴，但不做最终担保。
                        </p>
                    </section>

                    {/* 5. 免责声明 */}
                    <section className="bg-amber-50 rounded-xl p-6 border border-amber-100">
                        <h2 className="flex items-center text-xl font-bold text-gray-900 mb-4">
                            <AlertTriangle className="w-5 h-5 mr-2 text-amber-600"/>
                            5. 免责声明
                        </h2>
                        <div className="space-y-4 text-sm text-gray-700">
                            <div>
                                <h3 className="font-bold text-gray-900">A. AI 生成内容的准确性</h3>
                                <p>
                                    AI 模型生成的内容（包括代码、注释、文档等）基于概率预测，可能存在错误、遗漏或不准确之处。
                                    <strong className="block mt-1">用户必须在使用前对生成内容进行人工审查和测试。</strong>
                                    开发者不对因直接使用 AI 生成代码而导致的 Bug、安全漏洞、数据丢失或业务损失承担任何责任。
                                </p>
                            </div>

                            <div>
                                <h3 className="font-bold text-gray-900">B. 软件按“原样”提供</h3>
                                <p>
                                    本插件按“原样”和“现有”基础提供，不附带任何形式的明示或暗示担保，包括但不限于适销性、特定用途适用性和不侵权的担保。
                                </p>
                            </div>

                            <div>
                                <h3 className="font-bold text-gray-900">C. 数据安全风险</h3>
                                <p>
                                    虽然我们采取了合理的安全措施，但互联网传输本质上无法保证 100%
                                    安全。对于因不可抗力、黑客攻击、系统病毒、用户自身配置不当或第三方服务漏洞导致的数据泄露或丢失，我们不承担法律责任。
                                </p>
                            </div>
                        </div>
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

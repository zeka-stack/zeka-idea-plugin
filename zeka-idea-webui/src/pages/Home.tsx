import React from 'react';
import {ArrowRight, Bot, CheckCircle2, Code2, Database, FileText, GitBranch, Layout, Terminal, Zap} from 'lucide-react';

const FeatureCard = ({icon: Icon, title, description, badge}: {
    icon: React.ElementType,
    title: string,
    description: string,
    badge?: string
}) => (
    <div className="bg-white p-6 rounded-xl border border-gray-100 shadow-sm hover:shadow-md transition-all hover:-translate-y-1">
        <div className="flex items-start justify-between mb-4">
            <div className="p-3 bg-indigo-50 rounded-lg">
                <Icon className="w-6 h-6 text-indigo-600"/>
            </div>
            {badge && (
                <span className="bg-indigo-100 text-indigo-700 text-xs font-semibold px-2 py-1 rounded-full">
          {badge}
        </span>
            )}
        </div>
        <h3 className="text-lg font-bold text-gray-900 mb-2">{title}</h3>
        <p className="text-gray-600 text-sm leading-relaxed">
            {description}
        </p>
    </div>
);

const CodeDemo = () => (
    <div className="bg-gray-900 rounded-lg shadow-2xl overflow-hidden font-mono text-sm border border-gray-800">
        <div className="flex items-center px-4 py-2 bg-gray-800 border-b border-gray-700">
            <div className="flex space-x-2">
                <div className="w-3 h-3 rounded-full bg-red-500"></div>
                <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
                <div className="w-3 h-3 rounded-full bg-green-500"></div>
            </div>
            <div className="ml-4 text-gray-400 text-xs">UserService.java</div>
        </div>
        <div className="p-6 text-gray-300">
            <p><span className="text-purple-400">public</span> <span className="text-yellow-400">class </span>
                <span className="text-blue-400">UserService</span> <span className="text-yellow-400">{`{`}</span></p>
            <p className="ml-4 text-gray-500">// Zeka AI: Generating user validation logic...</p>
            <p className="ml-4"><span className="text-purple-400">public</span> <span className="text-blue-400">User </span>
                <span className="text-yellow-300">createUser</span>(String name, String
                email) <span className="text-yellow-400">{`{`}</span></p>
            <div className="ml-8 bg-indigo-900/30 -mx-4 px-4 py-1 border-l-2 border-indigo-500">
                <p><span className="text-purple-400">if</span> (name == <span className="text-purple-400">null</span> ||
                    name.isEmpty()) <span className="text-yellow-400">{`{`}</span></p>
                <p className="ml-4"><span className="text-purple-400">throw</span> <span className="text-purple-400">new </span>
                    <span className="text-yellow-300">IllegalArgumentException</span>(<span className="text-green-300">"Name cannot be empty"</span>);
                </p>
                <p><span className="text-yellow-400">{`}`}</span></p>
            </div>
            <p className="ml-4">...</p>
            <p><span className="text-yellow-400">{`}`}</span></p>
        </div>
    </div>
);

export const Home: React.FC = () => {
    const [version, setVersion] = React.useState<string>('v1.0.0');
    const [status, setStatus] = React.useState<'initial' | 'success' | 'error'>('initial');

    React.useEffect(() => {
        const fetchVersion = async () => {
            try {
                const response = await fetch('/api/plugin/version');
                if (response.ok) {
                    const text = await response.text();
                    setVersion('v' + text.trim());
                    setStatus('success');
                } else {
                    setStatus('error');
                }
            } catch {
                setStatus('error');
            }
        };

        fetchVersion();
        const interval = setInterval(fetchVersion, 60000);

        return () => clearInterval(interval);
    }, []);

    const getDotColors = () => {
        if (status === 'success') {
            return {ping: 'bg-green-400', dot: 'bg-green-500'};
        } else if (status === 'error') {
            return {ping: 'bg-red-400', dot: 'bg-red-500'};
        }
        return {ping: 'bg-indigo-400', dot: 'bg-indigo-500'};
    };

    const dotColors = getDotColors();

    return (
        <div className="flex flex-col">
            {/* Hero Section */}
            <section className="relative overflow-hidden pt-16 pb-20 lg:pt-24 lg:pb-32 bg-white">
                <div className="max-w-[1400px] mx-auto px-4 sm:px-6 relative z-10">
                    <div className="flex flex-col lg:flex-row items-center gap-16">
                        <div className="flex-1 text-center lg:text-left">
                            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 border border-indigo-100 text-indigo-700 text-sm font-medium mb-6">
                <span className="relative flex h-2 w-2">
                  <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${dotColors.ping}`}></span>
                  <span className={`relative inline-flex rounded-full h-2 w-2 ${dotColors.dot}`}></span>
                </span>
                                {version} Now Available
                            </div>
                            <h1 className="text-4xl lg:text-6xl font-extrabold text-gray-900 tracking-tight mb-6 leading-tight">
                                Zeka Stack <br/>
                                <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-purple-600">
                  Intelli IDEA Plugin
                </span>
                            </h1>
                            <p className="text-xl text-gray-500 mb-8 max-w-2xl mx-auto lg:mx-0 leading-relaxed">
                                您的终极 IDE 伴侣。集成 AI 编码引擎、Changelog 生成、Javadoc 自动生成、 Nacos 配置中心等使用插件，重新定义开发效率。
                            </p>
                            <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4">
                                <button className="w-full sm:w-auto px-8 py-3.5 bg-indigo-600 text-white rounded-lg font-bold hover:bg-indigo-700 transition-colors shadow-lg shadow-indigo-200 flex items-center justify-center gap-2">
                                    <Layout className="w-5 h-5"/>
                                    安装插件
                                </button>
                                <button className="w-full sm:w-auto px-8 py-3.5 bg-white text-gray-700 border border-gray-200 rounded-lg font-bold hover:bg-gray-50 transition-colors flex items-center justify-center gap-2">
                                    <GitBranch className="w-5 h-5"/>
                                    GitHub 源码
                                </button>
                            </div>
                            <div className="mt-8 flex items-center justify-center lg:justify-start gap-6 text-sm text-gray-500">
                                <div className="flex items-center gap-2">
                                    <CheckCircle2 className="w-4 h-4 text-emerald-500"/>
                                    <span>JetBrains 官方认证</span>
                                </div>
                                <div className="flex items-center gap-2">
                                    <CheckCircle2 className="w-4 h-4 text-emerald-500"/>
                                    <span>免费开源</span>
                                </div>
                            </div>
                        </div>

                        <div className="flex-1 w-full max-w-lg lg:max-w-none">
                            <div className="relative">
                                <div className="absolute -inset-4 bg-gradient-to-r from-indigo-500 to-purple-500 rounded-2xl opacity-20 blur-2xl animate-pulse"></div>
                                <CodeDemo/>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* Features Grid */}
            <section className="py-20 bg-[#F9FAFB]">
                <div className="max-w-[1400px] mx-auto px-4 sm:px-6">
                    <div className="text-center mb-16">
                        <h2 className="text-3xl font-bold text-gray-900 mb-4">全栈开发工具箱</h2>
                        <p className="text-gray-500 max-w-2xl mx-auto">
                            Zeka Plugin 不仅仅是 AI，它包含了后端开发所需的各类实用工具，无缝集成在您的 IDE 中。
                        </p>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                        <FeatureCard
                            icon={Bot}
                            title="Intelli AI Engine"
                            description="基于上下文的智能代码补全、自然语言生成代码、单元测试生成以及代码解释。让 AI 成为您的结对编程伙伴。"
                            badge="Core"
                        />
                        <FeatureCard
                            icon={FileText}
                            title="Javadoc Generator"
                            description="一键生成标准的 Java 文档注释。支持自定义模板，自动解析方法签名、参数和返回值，保持文档与代码同步。"
                        />
                        <FeatureCard
                            icon={Layout}
                            title="Swagger Manager"
                            description="直接在 IDE 中浏览、搜索和测试 Swagger/OpenAPI 接口。支持接口文档预览和 Mock 数据生成。"
                        />
                        <FeatureCard
                            icon={Database}
                            title="Nacos Config"
                            description="无需离开编辑器即可管理 Nacos 配置。支持多环境切换、配置版本对比和快速修改发布。"
                        />
                        <FeatureCard
                            icon={Zap}
                            title="Trace Analysis"
                            description="集成分布式链路追踪分析，快速定位微服务调用链中的性能瓶颈和异常节点。"
                        />
                        <FeatureCard
                            icon={Terminal}
                            title="Dev Helper"
                            description="内置常用开发工具集：JSON 格式化、Base64 编解码、时间戳转换、正则测试等。"
                        />
                    </div>
                </div>
            </section>

            {/* AI Agent Section */}
            <section className="py-20 bg-white border-t border-gray-100">
                <div className="max-w-[1400px] mx-auto px-4 sm:px-6">
                    <div className="flex flex-col lg:flex-row items-center gap-12">
                        <div className="flex-1 order-2 lg:order-1">
                            <div className="grid grid-cols-2 gap-4">
                                <div className="bg-indigo-50 p-6 rounded-2xl rounded-tr-[4rem]">
                                    <Bot className="w-10 h-10 text-indigo-600 mb-4"/>
                                    <h3 className="font-bold text-gray-900 mb-2">智能体</h3>
                                    <p className="text-sm text-gray-600">自主规划任务，执行复杂重构</p>
                                </div>
                                <div className="bg-purple-50 p-6 rounded-2xl rounded-tl-[4rem] mt-8">
                                    <Code2 className="w-10 h-10 text-purple-600 mb-4"/>
                                    <h3 className="font-bold text-gray-900 mb-2">代码理解</h3>
                                    <p className="text-sm text-gray-600">深度解析项目结构与依赖关系</p>
                                </div>
                            </div>
                        </div>
                        <div className="flex-1 order-1 lg:order-2">
                            <h2 className="text-3xl font-bold text-gray-900 mb-6">
                                下一代开发体验：<br/>
                                <span className="text-indigo-600">Intelli AI Agent</span>
                            </h2>
                            <p className="text-lg text-gray-500 mb-6 leading-relaxed">
                                超越简单的自动补全。Zeka Agent 能够理解您的自然语言指令，分析整个项目代码库，自主规划并执行复杂的修改任务。从修复
                                Bug 到功能迁移，一气呵成。
                            </p>
                            <ul className="space-y-4">
                                {['项目级上下文感知', '自动化重构与迁移', '智能 Bug 诊断与修复'].map((item, i) => (
                                    <li key={i} className="flex items-center gap-3">
                                        <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center">
                                            <CheckCircle2 className="w-4 h-4 text-green-600"/>
                                        </div>
                                        <span className="text-gray-700 font-medium">{item}</span>
                                    </li>
                                ))}
                            </ul>
                            <button className="mt-8 flex items-center gap-2 text-indigo-600 font-bold hover:text-indigo-700 group">
                                探索 Agent 能力 <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1"/>
                            </button>
                        </div>
                    </div>
                </div>
            </section>

            {/* CTA Section */}
            <section className="py-20 bg-indigo-900 text-white text-center">
                <div className="max-w-4xl mx-auto px-4">
                    <h2 className="text-3xl md:text-4xl font-bold mb-6">准备好提升开发效率了吗？</h2>
                    <p className="text-indigo-200 text-lg mb-10 max-w-2xl mx-auto">
                        加入数千名使用 Zeka Stack 的开发者行列，体验更智能、更流畅的编码工作流。
                    </p>
                    <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
                        <button className="px-8 py-4 bg-white text-indigo-900 rounded-lg font-bold hover:bg-indigo-50 transition-colors w-full sm:w-auto">
                            免费下载插件
                        </button>
                        <button className="px-8 py-4 bg-indigo-800 text-white border border-indigo-700 rounded-lg font-bold hover:bg-indigo-700 transition-colors w-full sm:w-auto">
                            查看文档
                        </button>
                    </div>
                </div>
            </section>
        </div>
    );
};

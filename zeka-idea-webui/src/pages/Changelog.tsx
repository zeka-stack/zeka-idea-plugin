import React, {useEffect, useState} from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {History} from 'lucide-react';

export const Changelog: React.FC = () => {
    const [content, setContent] = useState('');

    useEffect(() => {
        fetch('/CHANGELOG.md')
            .then(res => res.text())
            .then(text => setContent(text))
            .catch(err => console.error('Failed to load changelog:', err));
    }, []);

    return (
        <div className="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8 font-sans">
            <div className="max-w-4xl mx-auto">
                <div className="flex items-center gap-3 mb-8">
                    <div className="p-3 bg-indigo-100 rounded-lg">
                        <History className="w-8 h-8 text-indigo-600"/>
                    </div>
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900">更新日志</h1>
                        <p className="text-gray-500 mt-1">查看 IntelliAI 插件的历史版本更新记录</p>
                    </div>
                </div>

                <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden p-8">
                    <article className="prose prose-indigo max-w-none prose-headings:font-bold prose-h2:text-2xl prose-h2:mt-12 prose-h2:mb-6 prose-h2:pb-2 prose-h2:border-b prose-h2:border-gray-100 prose-h3:text-lg prose-h3:text-indigo-600 prose-ul:list-disc prose-li:my-1">
                        <ReactMarkdown
                            remarkPlugins={[remarkGfm]}
                            components={{
                                h2: ({node, ...props}) => {
                                    // Extract version and date if possible: "## [1.0.0] - 2024-01-01"
                                    // This is a simple styling wrapper
                                    return <h2 className="flex items-center gap-2 text-gray-900" {...props} />
                                },
                                li: ({node, ...props}) => (
                                    <li className="text-gray-600" {...props} />
                                )
                            }}
                        >
                            {content}
                        </ReactMarkdown>
                    </article>
                </div>
            </div>
        </div>
    );
};

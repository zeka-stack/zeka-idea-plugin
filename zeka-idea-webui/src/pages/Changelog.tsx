import React, {useEffect, useState} from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/github.css';
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
                    <style>{`
                        .markdown-body pre {
                            background-color: #f6f8fa;
                            border: 1px solid #e1e4e8;
                            border-radius: 6px;
                            padding: 16px;
                            overflow-x: auto;
                            font-size: 85%;
                            line-height: 1.45;
                            margin: 0;
                        }
                        .markdown-body pre code {
                            background-color: transparent;
                            border: 0;
                            padding: 0;
                            font-size: 100%;
                            word-break: normal;
                            white-space: pre;
                            display: block;
                        }
                        .markdown-body pre code.hljs {
                            padding: 0;
                            background: transparent;
                        }
                        .markdown-body code {
                            background-color: rgba(27, 31, 35, 0.05);
                            border-radius: 3px;
                            font-size: 85%;
                            padding: 0.2em 0.4em;
                            color: #e83e8c;
                        }
                        .markdown-body table {
                            border-spacing: 0;
                            border-collapse: collapse;
                        }
                        .markdown-body table tr {
                            background-color: #fff;
                            border-top: 1px solid #d0d7de;
                        }
                        .markdown-body table tr:nth-child(2n) {
                            background-color: #f6f8fa;
                        }
                        .markdown-body table th,
                        .markdown-body table td {
                            padding: 6px 13px;
                            border: 1px solid #d0d7de;
                        }
                        .markdown-body table th {
                            font-weight: 600;
                            background-color: #f6f8fa;
                        }
                        .markdown-body blockquote {
                            padding: 0 1em;
                            color: #656d76;
                            border-left: 0.25em solid #d0d7de;
                        }
                        .markdown-body ul,
                        .markdown-body ol {
                            padding-left: 2em;
                        }
                        .markdown-body li {
                            margin-top: 0.25em;
                        }
                        .markdown-body li > p {
                            margin-top: 16px;
                        }
                        .markdown-body li + li {
                            margin-top: 0.25em;
                        }
                        .markdown-body img {
                            max-width: 100%;
                            box-sizing: content-box;
                            background-color: #fff;
                        }
                        .markdown-body a {
                            color: #0969da;
                            text-decoration: none;
                        }
                        .markdown-body a:hover {
                            text-decoration: underline;
                        }
                        .markdown-body input[type="checkbox"] {
                            margin-right: 0.5em;
                            cursor: default;
                        }
                        .markdown-body li > input[type="checkbox"] {
                            margin-top: 0.25em;
                        }
                    `}</style>
                    <div className="markdown-body">
                        <ReactMarkdown
                            remarkPlugins={[remarkGfm]}
                            rehypePlugins={[rehypeHighlight]}
                            components={{
                                // Headings
                                h1: ({...props}) =>
                                    <h1 className="text-2xl font-bold mt-6 mb-4 pb-2 border-b border-gray-200 text-gray-900" {...props} />,
                                h2: ({...props}) =>
                                    <h2 className="text-xl font-bold mt-5 mb-3 pb-2 border-b border-gray-200 text-gray-900" {...props} />,
                                h3: ({...props}) => <h3 className="text-lg font-semibold mt-4 mb-2 text-gray-900" {...props} />,
                                h4: ({...props}) => <h4 className="text-base font-semibold mt-3 mb-2 text-gray-900" {...props} />,
                                h5: ({...props}) => <h5 className="text-sm font-semibold mt-2 mb-1 text-gray-900" {...props} />,
                                h6: ({...props}) => <h6 className="text-sm font-semibold mt-2 mb-1 text-gray-600" {...props} />,
                                // Paragraph
                                p: ({...props}) => <p className="mb-4 text-gray-700 leading-7" {...props} />,
                                // Lists
                                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                ul: ({node, ...props}: any) => {
                                    const isTaskList = node?.children?.some((child: unknown) =>
                                        (child as { children?: Array<{ type?: string }> })?.children?.some((c: {
                                            type?: string
                                        }) => c?.type === 'input')
                                    );
                                    return (
                                        <ul className={`mb-4 ml-6 space-y-1 ${isTaskList ? 'list-none' : 'list-disc'}`} {...props} />
                                    );
                                },
                                ol: ({...props}: React.ComponentPropsWithoutRef<'ol'>) =>
                                    <ol className="mb-4 ml-6 list-decimal space-y-1" {...props} />,
                                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                li: ({node, ...props}: any) => {
                                    const isTaskItem = (node as { children?: Array<{ type?: string }> })?.children?.some((child: {
                                        type?: string
                                    }) => child?.type === 'input');
                                    return (
                                        <li className={`text-gray-700 leading-7 ${isTaskItem ? 'flex items-start gap-2' : ''}`} {...props} />
                                    );
                                },
                                input: ({...props}: React.ComponentPropsWithoutRef<'input'>) => (
                                    <input
                                        type="checkbox"
                                        className="mt-1.5 mr-2 flex-shrink-0"
                                        disabled
                                        {...props}
                                    />
                                ),
                                // Code blocks
                                pre: ({children, ...props}: React.ComponentPropsWithoutRef<'pre'>) => (
                                    <pre className="mb-4 rounded-lg overflow-x-auto border border-gray-200" {...props}>
                                        {children}
                                    </pre>
                                ),
                                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                code: ({inline, className, children, ...props}: any) => {
                                    if (inline) {
                                        return (
                                            <code className="px-1.5 py-0.5 bg-pink-50 text-pink-700 rounded text-sm font-mono" {...props}>
                                                {children}
                                            </code>
                                        );
                                    }
                                    return (
                                        <code className={className} {...props}>
                                            {children}
                                        </code>
                                    );
                                },
                                // Blockquote
                                blockquote: ({...props}) => (
                                    <blockquote className="pl-4 border-l-4 border-gray-300 italic text-gray-600 mb-4" {...props} />
                                ),
                                // Links
                                a: ({...props}) => (
                                    <a className="text-[#0969da] hover:text-[#0550ae] hover:underline" target="_blank" rel="noopener noreferrer" {...props} />
                                ),
                                // Images
                                img: ({...props}) => (
                                    <img className="max-w-full h-auto rounded-lg my-4" {...props} />
                                ),
                                // Tables
                                table: ({...props}) => (
                                    <div className="overflow-x-auto my-4">
                                        <table className="min-w-full border-collapse border border-gray-300" {...props} />
                                    </div>
                                ),
                                thead: ({...props}) => (
                                    <thead className="bg-gray-50" {...props} />
                                ),
                                th: ({...props}) => (
                                    <th className="px-4 py-2 text-left border border-gray-300 font-semibold text-gray-900" {...props} />
                                ),
                                td: ({...props}) => (
                                    <td className="px-4 py-2 border border-gray-300 text-gray-700" {...props} />
                                ),
                                // Horizontal rule
                                hr: ({...props}) => (
                                    <hr className="my-6 border-t border-gray-200" {...props} />
                                ),
                                // Strong and emphasis
                                strong: ({...props}) => (
                                    <strong className="font-semibold text-gray-900" {...props} />
                                ),
                                em: ({...props}) => (
                                    <em className="italic" {...props} />
                                ),
                            }}
                        >
                            {content}
                        </ReactMarkdown>
                    </div>
                </div>
            </div>
        </div>
    );
};

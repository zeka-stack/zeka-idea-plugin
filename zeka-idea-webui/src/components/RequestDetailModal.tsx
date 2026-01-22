import React, {useCallback, useEffect, useState} from 'react';
import {ChevronUp, Maximize2, MessageSquare, Minimize2, Send, Trash2, X} from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/github.css';
import type {Comment, Request} from '../data';
import {api, type AuthStatus, formatDate, formatDateTime} from '../lib/api';

interface RequestDetailModalProps {
    request: Request | null;
    onClose: () => void;
    onVote: (id: number) => void;
    onCommentAdded: () => void;
    onDeleted?: (id: number) => void;
}

const statusColors = {
    'Planned': 'bg-blue-100 text-blue-700 border-blue-200',
    'In Progress': 'bg-amber-100 text-amber-700 border-amber-200',
    'Complete': 'bg-emerald-100 text-emerald-700 border-emerald-200',
    'Under Review': 'bg-purple-100 text-purple-700 border-purple-200',
    'Open': 'bg-gray-100 text-gray-600 border-gray-200',
};

export const RequestDetailModal: React.FC<RequestDetailModalProps> = ({request, onClose, onVote, onCommentAdded, onDeleted}) => {
    const [commentText, setCommentText] = useState('');
    const [comments, setComments] = useState<Comment[]>([]);
    const [loadingComments, setLoadingComments] = useState(false);
    const [isMaximized, setIsMaximized] = useState(false);
    const [authStatus, setAuthStatus] = useState<AuthStatus | null>(null);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);

    const fetchComments = useCallback(async (requestId: number) => {
        setLoadingComments(true);
        try {
            const data = await api.getComments(requestId);
            setComments(data);
        } catch (e) {
            console.error(e);
        } finally {
            setLoadingComments(false);
        }
    }, []);

    useEffect(() => {
        if (request) {
            fetchComments(request.id);
        }
    }, [request, fetchComments]);

    useEffect(() => {
        // Fetch auth status to check if user is admin
        api.getAuthStatus().then(status => {
            setAuthStatus(status);
        }).catch(console.error);
    }, []);

    useEffect(() => {
        const handleEsc = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', handleEsc);
        return () => window.removeEventListener('keydown', handleEsc);
    }, [onClose]);

    // Reset maximized state when opening a new request
    useEffect(() => {
        if (request) setIsMaximized(false);
    }, [request]);

    if (!request) return null;

    const handleSubmitComment = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!commentText.trim()) return;

        try {
            await api.createComment({feedbackId: request.id, content: commentText});
            setCommentText('');
            fetchComments(request.id);
            onCommentAdded();
        } catch (e) {
            console.error(e);
        }
    };

    const isAdmin = authStatus?.loggedIn === true && authStatus?.user?.role === 'admin';

    const handleDelete = async () => {
        if (!request) return;
        setIsDeleting(true);
        try {
            await api.deleteFeedback(request.id);
            if (onDeleted) {
                onDeleted(request.id);
            }
            onClose();
        } catch (e) {
            console.error('Failed to delete feedback:', e);
            alert('删除失败，请稍后重试');
        } finally {
            setIsDeleting(false);
            setShowDeleteConfirm(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm animate-in fade-in duration-200">
            <div className={`bg-white rounded-xl shadow-2xl w-full flex flex-col overflow-hidden animate-in zoom-in-95 duration-200 transition-all ${
                isMaximized ? 'max-w-6xl h-[90vh]' : 'max-w-3xl h-[60vh] min-h-[400px]'
            }`}>

                {/* Header */}
                <div className="flex items-start justify-between px-6 py-4 border-b border-gray-100 bg-gray-50/50 flex-shrink-0">
                    <div className="flex-1 pr-6">
                        <div className="flex items-center gap-2.5 mb-2">
                            <span className="text-xs font-mono text-gray-400">#{request.id}</span>
                            <span className={`px-2 py-0.5 text-[10px] font-bold uppercase rounded-full border ${statusColors[request.status]}`}>
                  {request.status}
               </span>
                            <span className="text-xs font-medium text-gray-400">{formatDate(request.createTime)}</span>
                        </div>
                        <h2 className="text-xl font-bold text-gray-900 leading-tight">{request.title}</h2>
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0">
                        {isAdmin && (
                            <button
                                onClick={() => setShowDeleteConfirm(true)}
                                className="text-red-400 hover:text-red-600 hover:bg-red-50 p-1.5 rounded-lg transition-colors"
                                title="删除需求"
                            >
                                <Trash2 className="w-5 h-5"/>
                            </button>
                        )}
                        <button
                            onClick={() => setIsMaximized(!isMaximized)}
                            className="text-gray-400 hover:text-gray-600 hover:bg-gray-100 p-1.5 rounded-lg transition-colors"
                            title={isMaximized ? "Restore" : "Maximize"}
                        >
                            {isMaximized ? <Minimize2 className="w-5 h-5"/> : <Maximize2 className="w-5 h-5"/>}
                        </button>
                        <button
                            onClick={onClose}
                            className="text-gray-400 hover:text-gray-600 hover:bg-gray-100 p-1.5 rounded-lg transition-colors"
                        >
                            <X className="w-5 h-5"/>
                        </button>
                    </div>
                </div>

                {/* Main Content */}
                <div className="flex flex-col lg:flex-row flex-1 overflow-hidden">

                    {/* Left Panel: Details */}
                    <div className={`flex-1 flex flex-col min-w-0 ${!isMaximized ? 'lg:border-r border-gray-200' : ''}`}>
                        <div className="flex-1 overflow-y-auto p-6">
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
                                /* Override highlight.js styles for better integration */
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
                                        // Code blocks - pre handles the container
                                        pre: ({children, ...props}: React.ComponentPropsWithoutRef<'pre'>) => (
                                            <pre className="mb-4 rounded-lg overflow-x-auto border border-gray-200" {...props}>
                                                {children}
                                            </pre>
                                        ),
                                        // Code - handles both inline and block code
                                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                        code: ({inline, className, children, ...props}: any) => {
                                            if (inline) {
                                                return (
                                                    <code className="px-1.5 py-0.5 bg-pink-50 text-pink-700 rounded text-sm font-mono" {...props}>
                                                        {children}
                                                    </code>
                                                );
                                            }
                                            // Block code - rehype-highlight will handle the highlighting
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
                                    {request.description || ''}
                                </ReactMarkdown>
                            </div>
                        </div>

                        {/* Voting - Fixed at bottom of left panel */}
                        {!isMaximized && (
                            <div className="px-6 py-4 border-t border-gray-200 bg-gray-50/30 flex-shrink-0 h-[82px] flex items-center">
                                <div className="flex items-center justify-between w-full">
                                    <div className="text-indigo-900 text-sm">
                                        <span className="font-bold text-lg">{request.voteCount}</span> votes
                                    </div>
                                    <button
                                        onClick={() => onVote(request.id)}
                                        className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-semibold hover:bg-indigo-700 transition-all shadow-sm active:scale-95"
                                    >
                                        <ChevronUp className="w-4 h-4"/>
                                        <span>Upvote</span>
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Right Panel: Comments */}
                    {!isMaximized && (
                        <div className="lg:w-80 flex flex-col flex-shrink-0 border-t lg:border-t-0 border-gray-200 bg-gray-50/50">
                            <div className="px-4 py-3 border-b border-gray-200 bg-gray-50/50 flex items-center justify-between flex-shrink-0">
                                <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
                                    <MessageSquare className="w-3.5 h-3.5"/>
                                    Discussion ({request.commentCount})
                            </h3>
                        </div>

                            {/* Comments List */}
                            <div className="flex-1 overflow-y-auto p-4 space-y-3">
                                {loadingComments ? (
                                    <div className="text-center py-4 text-gray-400 text-sm">Loading comments...</div>
                                ) : comments.length > 0 ? (
                                    comments.map((comment) => (
                                    <div key={comment.id} className="group">
                                        <div className="flex justify-between items-center mb-1.5">
                                            <span className="text-[10px] text-gray-400 font-medium">{formatDateTime(comment.createTime)}</span>
                                        </div>
                                        <div className="text-sm text-gray-700 leading-relaxed break-words whitespace-pre-wrap">
                                            {comment.content}
                                        </div>
                                    </div>
                                ))
                            ) : (
                                    <div className="flex flex-col items-center justify-center h-32 text-gray-400 text-sm py-4">
                                        <p>No comments yet</p>
                                </div>
                            )}
                        </div>

                            {/* Comment Form */}
                            <div className="p-4 border-t border-gray-200 bg-gray-50/30 flex-shrink-0 h-[82px] flex items-center">
                                <form onSubmit={handleSubmitComment} className="relative w-full">
                                    <div className="absolute -top-3 right-0">
                                    <span className={`text-[9px] font-medium ${commentText.length > 1000 ? 'text-red-500' : 'text-gray-400'}`}>
                                        {commentText.length}/1000
                                    </span>
                                    </div>
                        <textarea
                            value={commentText}
                            onChange={(e) => setCommentText(e.target.value)}
                            maxLength={1000}
                            placeholder="Add a comment..."
                            className="w-full pl-3 pr-10 py-2 bg-white border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all resize-none"
                            rows={2}
                        />
                                <button
                                    type="submit"
                                    disabled={!commentText.trim()}
                                    className="absolute right-2 top-1/2 -translate-y-1/2 p-2 bg-indigo-600 text-white hover:bg-indigo-700 rounded-lg transition-all disabled:opacity-30 disabled:bg-gray-300 shadow-sm active:scale-95"
                                >
                                    <Send className="w-4 h-4"/>
                                </button>
                            </form>
                        </div>
                    </div>
                    )}

                </div>
            </div>

            {/* Delete Confirmation Dialog */}
            {showDeleteConfirm && (
                <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-6 animate-in zoom-in-95 duration-200">
                        <h3 className="text-lg font-bold text-gray-900 mb-2">确认删除</h3>
                        <p className="text-gray-600 mb-6">
                            确定要删除这个需求吗？此操作无法撤销。
                        </p>
                        <div className="flex gap-3 justify-end">
                            <button
                                onClick={() => setShowDeleteConfirm(false)}
                                disabled={isDeleting}
                                className="px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg font-medium transition-colors disabled:opacity-50"
                            >
                                取消
                            </button>
                            <button
                                onClick={handleDelete}
                                disabled={isDeleting}
                                className="px-4 py-2 text-white bg-red-600 hover:bg-red-700 rounded-lg font-medium transition-colors disabled:opacity-50 flex items-center gap-2"
                            >
                                {isDeleting ? (
                                    <>
                                        <span className="animate-spin">⏳</span>
                                        <span>删除中...</span>
                                    </>
                                ) : (
                                    '确认删除'
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

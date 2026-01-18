import React, {useState} from 'react';
import {ChevronUp, MessageSquare, Send, X} from 'lucide-react';
import type {Request} from '../data';

interface RequestDetailModalProps {
    request: Request | null;
    onClose: () => void;
    onVote: (id: string) => void;
    onAddComment: (requestId: string, content: string) => void;
}

const statusColors = {
    'Planned': 'bg-blue-100 text-blue-700 border-blue-200',
    'In Progress': 'bg-amber-100 text-amber-700 border-amber-200',
    'Complete': 'bg-emerald-100 text-emerald-700 border-emerald-200',
    'Under Review': 'bg-purple-100 text-purple-700 border-purple-200',
    'Open': 'bg-gray-100 text-gray-600 border-gray-200',
};

export const RequestDetailModal: React.FC<RequestDetailModalProps> = ({request, onClose, onVote, onAddComment}) => {
    const [commentText, setCommentText] = useState('');

    if (!request) return null;

    const handleSubmitComment = (e: React.FormEvent) => {
        e.preventDefault();
        if (!commentText.trim()) return;

        onAddComment(request.id, commentText);
        setCommentText('');
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-white rounded-xl shadow-2xl w-full max-w-3xl h-[42vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-200">

                {/* Header - Fixed */}
                <div className="flex items-start justify-between px-4 py-3 border-b border-gray-100 bg-gray-50/50 flex-shrink-0">
                    <div className="flex-1 pr-6">
                        <div className="flex items-center gap-2.5 mb-1">
                            <span className="text-[10px] font-mono text-gray-400">#{request.id}</span>
                            <span className={`px-1.5 py-0.5 text-[9px] font-bold uppercase rounded-full border ${statusColors[request.status]}`}>
                  {request.status}
               </span>
                            <span className="text-[10px] font-medium text-gray-400">{request.date}</span>
                        </div>
                        <h2 className="text-base font-bold text-gray-900 leading-tight">{request.title}</h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-gray-600 hover:bg-gray-100 p-1 rounded-lg transition-colors flex-shrink-0"
                    >
                        <X className="w-4 h-4"/>
                    </button>
                </div>

                {/* Main Content - Flex Layout */}
                <div className="flex flex-col lg:flex-row flex-1 overflow-hidden">

                    {/* Left Panel: Details - Scrollable */}
                    <div className="flex-1 overflow-y-auto p-4 lg:border-r border-gray-100">
                        <div className="mb-4">
                            <p className="text-gray-600 leading-relaxed text-xs whitespace-pre-wrap">
                                {request.description}
                            </p>
                        </div>

                        {/* Voting Action */}
                        <div className="flex items-center justify-between p-2.5 bg-indigo-50/50 rounded-lg border border-indigo-100/50">
                            <div className="text-indigo-900 text-xs">
                                <span className="font-bold">{request.votes}</span> votes
                            </div>
                            <button
                                onClick={() => onVote(request.id)}
                                className="flex items-center gap-1.5 bg-indigo-600 text-white px-2.5 py-1.5 rounded-md text-[11px] font-semibold hover:bg-indigo-700 transition-all shadow-sm active:scale-95"
                            >
                                <ChevronUp className="w-3.5 h-3.5"/>
                                <span>Upvote</span>
                            </button>
                        </div>
                    </div>

                    {/* Right Panel: Comments - Flex Col */}
                    <div className="lg:w-60 bg-gray-50 flex flex-col flex-shrink-0 h-1/2 lg:h-auto border-t lg:border-t-0 border-gray-200">
                        <div className="px-3 py-2 border-b border-gray-200 bg-white/50 flex items-center justify-between flex-shrink-0">
                            <h3 className="text-[10px] font-bold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
                                <MessageSquare className="w-3 h-3"/>
                                Discussion ({request.comments})
                            </h3>
                        </div>

                        {/* Comments List - Scrollable */}
                        <div className="flex-1 overflow-y-auto p-3 space-y-2.5">
                            {request.commentsList && request.commentsList.length > 0 ? (
                                request.commentsList.map((comment) => (
                                    <div key={comment.id} className="group">
                                        <div className="flex justify-between items-center mb-0.5">
                                            <span className="text-[9px] text-gray-400 font-medium">{comment.date}</span>
                                        </div>
                                        <div className="text-[11px] text-gray-600 bg-white p-2 rounded border border-gray-200 shadow-sm leading-normal">
                                            {comment.content}
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <div className="flex flex-col items-center justify-center h-full text-gray-400 text-[10px] py-4">
                                    <p>No comments</p>
                                </div>
                            )}
                        </div>

                        {/* Comment Form - Fixed at bottom */}
                        <div className="p-2.5 bg-white border-t border-gray-200 flex-shrink-0">
                            <form onSubmit={handleSubmitComment} className="relative">
                        <textarea
                            value={commentText}
                            onChange={(e) => setCommentText(e.target.value)}
                            placeholder="Add comment..."
                            className="w-full pl-2 pr-8 py-1.5 bg-gray-50 border border-gray-200 rounded text-[11px] focus:outline-none focus:ring-1 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all resize-none"
                            rows={1}
                        />
                                <button
                                    type="submit"
                                    disabled={!commentText.trim()}
                                    className="absolute right-1 top-1 p-1 text-indigo-600 hover:bg-indigo-50 rounded transition-colors disabled:opacity-30"
                                >
                                    <Send className="w-3 h-3"/>
                                </button>
                            </form>
                        </div>
                    </div>

                </div>
            </div>
        </div>
    );
};

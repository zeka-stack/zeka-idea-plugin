import React from 'react';
import {ChevronUp, MessageSquare} from 'lucide-react';
import type {Request} from '../data';
import {formatDate} from '../lib/api';

interface RequestCardProps {
    request: Request;
    onClick: (request: Request) => void;
}

const statusColors = {
    'Planned': 'bg-blue-100 text-blue-700 border-blue-200',
    'In Progress': 'bg-amber-100 text-amber-700 border-amber-200',
    'Complete': 'bg-emerald-100 text-emerald-700 border-emerald-200',
    'Under Review': 'bg-purple-100 text-purple-700 border-purple-200',
    'Open': 'bg-gray-100 text-gray-600 border-gray-200',
};

export const RequestCard: React.FC<RequestCardProps> = ({request, onClick}) => {
    return (
        <div
            onClick={() => onClick(request)}
            className="bg-white rounded-xl p-5 border border-gray-200/60 shadow-sm hover:shadow-md hover:border-gray-300 transition-all duration-200 flex gap-5 items-start cursor-pointer group/card"
        >
            {/* Vote Button Column */}
            <div className="flex flex-col items-center min-w-[3rem] pt-1">
                <button
                    onClick={(e) => {
                        e.stopPropagation(); /* Add vote logic here later if needed */
                    }}
                    className="flex flex-col items-center justify-center w-12 h-14 rounded-xl border border-gray-200 bg-gray-50/50 hover:border-indigo-200 hover:bg-indigo-50/50 transition-all group"
                >
                    <ChevronUp className="w-6 h-6 text-gray-400 group-hover:text-indigo-500 transition-colors" strokeWidth={2.5}/>
                    <span className="text-sm font-bold text-gray-700 group-hover:text-indigo-600">{request.voteCount}</span>
                </button>
            </div>

            {/* Content Column */}
            <div className="flex-1 min-w-0">
                <div className="flex justify-between items-start">
                    <h3 className="text-[17px] font-semibold text-gray-900 leading-snug mb-1.5 group-hover/card:text-indigo-600 transition-colors">
                        {request.title}
                    </h3>
                </div>
                <p className="text-gray-500 text-[15px] leading-relaxed mb-3 line-clamp-2">
                    {request.description}
                </p>

                <div className="flex items-center flex-wrap gap-y-2 gap-x-3 text-xs font-medium text-gray-400">
           <span className={`px-2.5 py-0.5 rounded-full border ${statusColors[request.status]}`}>
            {request.status}
          </span>
                    <span className="flex items-center gap-1 hover:text-gray-600 transition-colors">
            <MessageSquare className="w-4 h-4"/>
                        {request.commentCount} Comments
          </span>
                    <span>•</span>
                    <span>{formatDate(request.createTime)}</span>
                </div>
            </div>
        </div>
    );
};
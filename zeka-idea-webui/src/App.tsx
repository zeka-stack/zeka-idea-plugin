import {useState} from 'react';
import {Header} from './components/Header';
import {RequestCard} from './components/RequestCard';
import {NewRequestModal} from './components/NewRequestModal';
import {RequestDetailModal} from './components/RequestDetailModal';
import type {Comment, Request} from './data';
import {requests as initialRequests} from './data';
import {CheckCircle2, Circle, Clock, Plus, Search} from 'lucide-react';

function App() {
    const [requests, setRequests] = useState(initialRequests);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedRequest, setSelectedRequest] = useState<Request | null>(null);

    const handleCreateRequest = (newReqData: Pick<Request, 'title' | 'description' | 'priority'>) => {
        const newRequest: Request = {
            id: Math.random().toString(36).substr(2, 9),
            ...newReqData,
            votes: 0,
            comments: 0,
            commentsList: [],
            status: 'Open',
            author: 'You',
            date: 'Just now'
        };

        setRequests([newRequest, ...requests]);
    };

    const handleCardClick = (request: Request) => {
        setSelectedRequest(request);
    };

    const handleVote = (id: string) => {
        const updatedRequests = requests.map(req =>
            req.id === id ? {...req, votes: req.votes + 1} : req
        );
        setRequests(updatedRequests);

        // Also update selectedRequest if it's open
        if (selectedRequest && selectedRequest.id === id) {
            setSelectedRequest({...selectedRequest, votes: selectedRequest.votes + 1});
        }
    };

    const handleAddComment = (requestId: string, content: string) => {
        const newComment: Comment = {
            id: Math.random().toString(36).substr(2, 9),
            author: 'You',
            avatar: 'Y',
            content,
            date: 'Just now'
        };

        const updatedRequests = requests.map(req => {
            if (req.id === requestId) {
                return {
                    ...req,
                    comments: req.comments + 1,
                    commentsList: [...req.commentsList, newComment]
                };
            }
            return req;
        });

        setRequests(updatedRequests);

        // Update selected request view immediately
        if (selectedRequest && selectedRequest.id === requestId) {
            setSelectedRequest({
                ...selectedRequest,
                comments: selectedRequest.comments + 1,
                commentsList: [...selectedRequest.commentsList, newComment]
            });
        }
    };

    // Group requests by status category
    const newRequests = requests.filter(r => ['Open', 'Under Review', 'Planned'].includes(r.status));
    const inProgressRequests = requests.filter(r => r.status === 'In Progress');
    const completedRequests = requests.filter(r => r.status === 'Complete');

    const Column = ({title, icon: Icon, items, countColor}: { title: string, icon: any, items: Request[], countColor: string }) => (
        <div className="flex flex-col h-full">
            <div className="flex items-center gap-2 mb-4 px-1">
                <Icon className={`w-5 h-5 ${countColor}`}/>
                <h2 className="font-bold text-gray-900">{title}</h2>
                <span className="bg-gray-100 text-gray-500 text-xs font-semibold px-2.5 py-0.5 rounded-full ml-auto">
          {items.length}
        </span>
            </div>
            <div className="flex-1 bg-gray-100/50 rounded-xl p-3 border border-gray-200/50 flex flex-col gap-3 min-h-[200px]">
                {items.map(req => (
                    <RequestCard
                        key={req.id}
                        request={req}
                        onClick={handleCardClick}
                    />
                ))}
                {items.length === 0 && (
                    <div className="flex flex-col items-center justify-center h-32 text-gray-400 text-sm italic">
                        No requests
                    </div>
                )}
            </div>
        </div>
    );

    return (
        <div className="min-h-screen bg-[#F9FAFB] pb-20 font-sans selection:bg-indigo-100 selection:text-indigo-900">
            <Header/>

            <main className="max-w-[1400px] mx-auto px-4 sm:px-6 py-10">
                {/* Page Header */}
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-10">
                    <div className="flex-1">
                        <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight mb-3">Feature Requests</h1>
                        <p className="text-[16px] text-gray-500 max-w-2xl">
                            Vote on existing requests or submit your own ideas to help us improve.
                        </p>
                    </div>
                    <div className="flex gap-3">
                        <div className="relative group hidden sm:block">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"/>
                            <input
                                type="text"
                                placeholder="Search..."
                                className="pl-9 pr-4 py-2.5 bg-white border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all shadow-sm"
                            />
                        </div>
                        <button
                            onClick={() => setIsModalOpen(true)}
                            className="flex items-center justify-center gap-2 bg-indigo-600 text-white px-5 py-2.5 rounded-lg font-semibold hover:bg-indigo-700 transition-all shadow-sm active:scale-95"
                        >
                            <Plus className="w-4 h-4"/>
                            <span>Submit Idea</span>
                        </button>
                    </div>
                </div>

                {/* Kanban Board */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
                    <Column
                        title="新需求"
                        icon={Circle}
                        items={newRequests}
                        countColor="text-gray-500"
                    />
                    <Column
                        title="处理中"
                        icon={Clock}
                        items={inProgressRequests}
                        countColor="text-amber-500"
                    />
                    <Column
                        title="已完成"
                        icon={CheckCircle2}
                        items={completedRequests}
                        countColor="text-emerald-500"
                    />
                </div>

                {/* Footer Text */}
                <div className="mt-16 text-center text-sm text-gray-400">
                    Powered by <a href="#" className="font-medium text-gray-600 hover:underline">Cool Request</a>
                </div>
            </main>

            <NewRequestModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSubmit={handleCreateRequest}
            />

            <RequestDetailModal
                request={selectedRequest}
                onClose={() => setSelectedRequest(null)}
                onVote={handleVote}
                onAddComment={handleAddComment}
            />
        </div>
    )
}

export default App

import {useCallback, useEffect, useRef, useState} from 'react';
import {RequestCard} from '../components/RequestCard';
import {NewRequestModal} from '../components/NewRequestModal';
import {RequestDetailModal} from '../components/RequestDetailModal';
import type {Request} from '../data';
import {api, type AuthStatus, type Project} from '../lib/api';
import {CheckCircle2, ChevronDown, Circle, Clock, Plus, Search} from 'lucide-react';

export const FeatureRequests = () => {
    const [requests, setRequests] = useState<Request[]>([]);
    const [projects, setProjects] = useState<Project[]>([]);
    const [selectedProject, setSelectedProject] = useState<Project | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedRequest, setSelectedRequest] = useState<Request | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [isProjectDropdownOpen, setIsProjectDropdownOpen] = useState(false);
    const projectDropdownRef = useRef<HTMLDivElement>(null);
    const [authStatus, setAuthStatus] = useState<AuthStatus | null>(null);
    const [draggedItem, setDraggedItem] = useState<Request | null>(null);

    const fetchRequests = useCallback(async () => {
        if (!selectedProject) return;
        try {
            const data = await api.getFeedbacks(selectedProject.id, searchQuery);
            // Map Feedback to Request (types are compatible via data.ts alias)
            setRequests(data as Request[]);
        } catch (e) {
            console.error(e);
        }
    }, [selectedProject, searchQuery]);

    useEffect(() => {
        // Fetch projects
        api.getProjects().then(data => {
            setProjects(data);
            if (data.length > 0) {
                setSelectedProject(data[0]);
            }
        }).catch(console.error);

        // Fetch auth status
        api.getAuthStatus().then(status => {
            console.log('Auth status:', status); // Debug log
            setAuthStatus(status);
        }).catch(console.error);
    }, []);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (projectDropdownRef.current && !projectDropdownRef.current.contains(event.target as Node)) {
                setIsProjectDropdownOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    useEffect(() => {
        if (selectedProject) {
            void fetchRequests();
        }
    }, [selectedProject, fetchRequests]);

    const handleCreateRequest = async (newReqData: Pick<Request, 'title' | 'description' | 'priority'>) => {
        if (!selectedProject) return;
        try {
            await api.createFeedback({
                projectId: selectedProject.id,
                title: newReqData.title,
                description: newReqData.description,
                priority: newReqData.priority
            });
            fetchRequests();
        } catch (e) {
            console.error(e);
        }
    };

    const handleCardClick = (request: Request) => {
        setSelectedRequest(request);
    };

    const handleVote = async (id: number) => {
        try {
            await api.voteFeedback(id);
            // Optimistic update or refetch
            setRequests(requests.map(req =>
                req.id === id ? {...req, voteCount: req.voteCount + 1} : req
            ));
            if (selectedRequest && selectedRequest.id === id) {
                setSelectedRequest({...selectedRequest, voteCount: selectedRequest.voteCount + 1});
            }
        } catch (e) {
            console.error(e);
        }
    };

    const handleCommentAdded = () => {
        // Refresh requests to update comment count
        fetchRequests();
        if (selectedRequest) {
            setSelectedRequest({
                ...selectedRequest,
                commentCount: selectedRequest.commentCount + 1
            });
        }
    };

    // 本地开发时允许拖拽，生产环境需要管理员权限
    const isAdmin = import.meta.env.DEV
        ? true
        : (authStatus?.loggedIn === true && authStatus?.user?.role === 'admin');

    // Debug log
    useEffect(() => {
        console.log('isAdmin:', isAdmin, 'authStatus:', authStatus, 'isDev:', import.meta.env.DEV);
    }, [isAdmin, authStatus]);

    const handleDragStart = (e: React.DragEvent, request: Request) => {
        if (!isAdmin) return;
        setDraggedItem(request);
        e.dataTransfer.effectAllowed = 'move';
    };

    const handleDragOver = (e: React.DragEvent) => {
        if (!isAdmin) return;
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
    };

    const handleDrop = async (e: React.DragEvent, targetStatus: string) => {
        if (!isAdmin || !draggedItem) return;
        e.preventDefault();

        // For "新需求" column, keep the original status if it's already in that category
        if (targetStatus === 'Open') {
            const newStatuses = ['Open', 'Under Review', 'Planned'];
            if (newStatuses.includes(draggedItem.status)) {
                setDraggedItem(null);
                return;
            }
            // Default to 'Open' when dropping to "新需求" column
            targetStatus = 'Open';
        } else if (draggedItem.status === targetStatus) {
            setDraggedItem(null);
            return;
        }

        try {
            await api.updateFeedbackStatus(draggedItem.id, targetStatus, {
                projectId: draggedItem.projectId,
                title: draggedItem.title,
                description: draggedItem.description,
                priority: draggedItem.priority,
            });

            // Optimistic update
            setRequests(requests.map(req =>
                req.id === draggedItem.id ? {...req, status: targetStatus as Request['status']} : req
            ));

            if (selectedRequest && selectedRequest.id === draggedItem.id) {
                setSelectedRequest({...selectedRequest, status: targetStatus as Request['status']});
            }
        } catch (error) {
            console.error('Failed to update status:', error);
        } finally {
            setDraggedItem(null);
        }
    };

    const handleDragEnd = () => {
        setDraggedItem(null);
    };

    // Group requests by status category
    const newRequests = requests.filter(r => ['Open', 'Under Review', 'Planned'].includes(r.status));
    const inProgressRequests = requests.filter(r => r.status === 'In Progress');
    const completedRequests = requests.filter(r => r.status === 'Complete');

    return (
        <div className="pb-20">
            <main className="max-w-[1400px] mx-auto px-4 sm:px-6 py-10">
                {/* Page Header */}
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-10">
                    <div className="flex-1">
                        <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight mb-3">Feature Requests</h1>
                        <p className="text-[16px] text-gray-500 max-w-2xl">
                            Vote on existing requests or submit your own ideas to help us improve.
                        </p>
                    </div>
                    <div className="flex gap-3 flex-col sm:flex-row mt-4 md:mt-0">
                        <div className="flex gap-3">
                            {/* Project Selector */}
                            <div className="relative" ref={projectDropdownRef}>
                                <div
                                    className="relative cursor-pointer min-w-[180px]"
                                    onClick={() => setIsProjectDropdownOpen(!isProjectDropdownOpen)}
                                >
                                    <div className="w-full px-3 py-2.5 pr-9 border border-gray-200 rounded-lg bg-white flex items-center text-sm transition-colors hover:border-indigo-300 shadow-sm">
                                        <span className="text-gray-700 font-medium">{selectedProject?.name || 'Select project'}</span>
                                    </div>
                                    <ChevronDown className={`absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none transition-transform duration-200 ${isProjectDropdownOpen ? 'rotate-180' : ''}`}/>
                                </div>

                                {isProjectDropdownOpen && (
                                    <div className="absolute z-20 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg py-1 max-h-60 overflow-y-auto">
                                        {projects.map((project) => (
                                            <div
                                                key={project.id}
                                                onClick={() => {
                                                    setSelectedProject(project);
                                                    setIsProjectDropdownOpen(false);
                                                }}
                                                className={`flex items-center gap-3 px-3 py-2.5 hover:bg-gray-50 cursor-pointer ${selectedProject?.id === project.id ? 'bg-indigo-50 text-indigo-700' : 'text-gray-700'}`}
                                            >
                                                <span className="text-sm font-medium">{project.name}</span>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div className="relative group hidden sm:block">
                                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"/>
                                <input
                                    type="text"
                                    placeholder="Search..."
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                    className="pl-9 pr-4 py-2.5 bg-white border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all shadow-sm"
                                />
                            </div>
                        </div>

                        <button
                            onClick={() => setIsModalOpen(true)}
                            className="flex items-center justify-center gap-2 bg-indigo-600 text-white px-5 py-2.5 rounded-lg font-semibold hover:bg-indigo-700 transition-all shadow-sm active:scale-95 whitespace-nowrap"
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
                        onCardClick={handleCardClick}
                        isAdmin={isAdmin}
                        onDragStart={handleDragStart}
                        onDragOver={handleDragOver}
                        onDrop={(e) => handleDrop(e, 'Open')}
                        onDragEnd={handleDragEnd}
                        targetStatuses={['Open', 'Under Review', 'Planned']}
                    />
                    <Column
                        title="处理中"
                        icon={Clock}
                        items={inProgressRequests}
                        countColor="text-amber-500"
                        onCardClick={handleCardClick}
                        isAdmin={isAdmin}
                        onDragStart={handleDragStart}
                        onDragOver={handleDragOver}
                        onDrop={(e) => handleDrop(e, 'In Progress')}
                        onDragEnd={handleDragEnd}
                        targetStatus="In Progress"
                    />
                    <Column
                        title="已完成"
                        icon={CheckCircle2}
                        items={completedRequests}
                        countColor="text-emerald-500"
                        onCardClick={handleCardClick}
                        isAdmin={isAdmin}
                        onDragStart={handleDragStart}
                        onDragOver={handleDragOver}
                        onDrop={(e) => handleDrop(e, 'Complete')}
                        onDragEnd={handleDragEnd}
                        targetStatus="Complete"
                    />
                </div>

                {/* Footer Text */}
                <div className="mt-16 text-center text-sm text-gray-400">
                    Powered by <a href="#" className="font-medium text-gray-600 hover:underline">dong4j</a>
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
                onCommentAdded={handleCommentAdded}
                onDeleted={(id) => {
                    setRequests(requests.filter(req => req.id !== id));
                    setSelectedRequest(null);
                }}
            />
        </div>
    )
}

interface ColumnProps {
    title: string;
    icon: React.ElementType;
    items: Request[];
    countColor: string;
    onCardClick: (r: Request) => void;
    isAdmin?: boolean;
    onDragStart?: (e: React.DragEvent, request: Request) => void;
    onDragOver?: (e: React.DragEvent) => void;
    onDrop?: (e: React.DragEvent) => void;
    onDragEnd?: () => void;
    targetStatus?: string;
    targetStatuses?: string[];
}

const Column = ({
                    title,
                    icon: Icon,
                    items,
                    countColor,
                    onCardClick,
                    isAdmin = false,
                    onDragStart,
                    onDragOver,
                    onDrop,
                    onDragEnd,
                }: ColumnProps) => (
    <div className="flex flex-col h-full">
        <div className="flex items-center gap-2 mb-4 px-1">
            <Icon className={`w-5 h-5 ${countColor}`}/>
            <h2 className="font-bold text-gray-900">{title}</h2>
            <span className="bg-gray-100 text-gray-500 text-xs font-semibold px-2.5 py-0.5 rounded-full ml-auto">
          {items.length}
        </span>
        </div>
        <div
            className={`flex-1 bg-gray-100/50 rounded-xl p-3 border border-gray-200/50 flex flex-col gap-3 min-h-[200px] ${isAdmin ? 'transition-colors' : ''}`}
            onDragOver={isAdmin ? onDragOver : undefined}
            onDrop={isAdmin ? onDrop : undefined}
        >
            {items.map(req => (
                <div
                    key={req.id}
                    draggable={isAdmin}
                    onDragStart={isAdmin && onDragStart ? (e) => onDragStart(e, req) : undefined}
                    onDragEnd={isAdmin ? onDragEnd : undefined}
                    className={isAdmin ? 'cursor-move active:opacity-50 transition-opacity' : ''}
                >
                    <RequestCard
                        request={req}
                        onClick={onCardClick}
                    />
                </div>
            ))}
            {items.length === 0 && (
                <div className="flex flex-col items-center justify-center h-32 text-gray-400 text-sm italic">
                    No requests
                </div>
            )}
        </div>
    </div>
);

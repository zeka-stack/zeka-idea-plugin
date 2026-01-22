import {authStorage} from './auth';

const BASE_URL = '/api';

export interface Project {
    id: number;
    key: string;
    name: string;
    description: string;
    icon: string;
    status: number;
}

export interface Feedback {
    id: number;
    projectId: number;
    title: string;
    description: string;
    issuesUrl?: string; // GitHub issues URL
    status: 'Open' | 'In Progress' | 'Complete' | 'Planned' | 'Under Review';
    priority: 'Low' | 'Medium' | 'High';
    voteCount: number;
    commentCount: number;
    createTime: string; // ISO string from backend
}

export interface UserAccount {
    id: number;
    githubId: number;
    githubLogin: string;
    githubName: string;
    avatarUrl: string;
    email: string;
    role: string;
    deviceId: string;
    lastLoginTime: string;
}

export interface AuthStatus {
    loggedIn: boolean;
    user: UserAccount | null;
}

export interface FeedbackComment {
    id: number;
    feedbackId: number;
    content: string;
    createTime: string;
}

export const api = {
    getProjects: async (): Promise<Project[]> => {
        const res = await fetch(`${BASE_URL}/projects/list?status=1`);
        if (!res.ok) throw new Error('Failed to fetch projects');
        const json = await res.json();
        return Array.isArray(json) ? json : (json.data || []);
    },

    getFeedbacks: async (projectId: number, search?: string): Promise<Feedback[]> => {
        let url = `${BASE_URL}/projects/feedbacks/list?projectId=${projectId}`;
        if (search) {
            url += `&title=${encodeURIComponent(search)}`;
        }
        const res = await fetch(url);
        if (!res.ok) throw new Error('Failed to fetch feedbacks');
        const json = await res.json();
        return Array.isArray(json) ? json : (json.data || []);
    },

    createFeedback: async (feedback: { projectId: number; title: string; description: string; priority: string }) => {
        const res = await fetch(`${BASE_URL}/projects/feedbacks`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({...feedback, status: 'Open'}),
        });
        if (!res.ok) throw new Error('Failed to create feedback');
    },

    voteFeedback: async (id: number) => {
        const res = await fetch(`${BASE_URL}/projects/feedbacks/${id}/vote`, {
            method: 'POST',
        });
        if (!res.ok) throw new Error('Failed to vote');
    },

    getComments: async (feedbackId: number): Promise<FeedbackComment[]> => {
        const res = await fetch(`${BASE_URL}/projects/feedback-comment/list?feedbackId=${feedbackId}`);
        if (!res.ok) throw new Error('Failed to fetch comments');
        const json = await res.json();
        return Array.isArray(json) ? json : (json.data || []);
    },

    createComment: async (comment: { feedbackId: number; content: string }) => {
        const res = await fetch(`${BASE_URL}/projects/feedback-comment`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(comment),
        });
        if (!res.ok) throw new Error('Failed to create comment');
    },

    getFeedbackDetail: async (id: number): Promise<Feedback> => {
        const res = await fetch(`${BASE_URL}/projects/feedbacks/${id}`);
        if (!res.ok) throw new Error('Failed to fetch feedback detail');
        const json = await res.json();
        return Array.isArray(json) ? json[0] : (json.data || json);
    },

    getAuthStatus: async (): Promise<AuthStatus | null> => {
        const token = authStorage.getToken();
        if (!token) return null;
        try {
            const res = await fetch(`${BASE_URL}/auth/me`, {
                headers: {'Authorization': `Bearer ${token}`},
            });
            if (!res.ok) return null;
            const json = await res.json();
            // 后端返回的数据结构是 { data: { loggedIn, user } }
            const data = json?.data ?? json;
            // 确保返回正确的结构
            if (data && typeof data === 'object' && 'loggedIn' in data) {
                return data as AuthStatus;
            }
            return null;
        } catch {
            return null;
        }
    },

    updateFeedbackStatus: async (id: number, status: string, feedback: Partial<Feedback>) => {
        const res = await fetch(`${BASE_URL}/projects/feedbacks/${id}`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                id,
                status,
                ...feedback,
            }),
        });
        if (!res.ok) throw new Error('Failed to update feedback status');
    },

    deleteFeedback: async (id: number) => {
        const res = await fetch(`${BASE_URL}/projects/feedbacks`, {
            method: 'DELETE',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify([id]),
        });
        if (!res.ok) throw new Error('Failed to delete feedback');
    },
};

export const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;
};

export const formatDateTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const ymd = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;
    const time = date.toTimeString().split(' ')[0];
    return `${ymd} ${time}`;
};

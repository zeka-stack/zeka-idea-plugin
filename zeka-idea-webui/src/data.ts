export interface Comment {
    id: string;
    author: string;
    avatar: string; // url or initial
    content: string;
    date: string;
}

export interface Request {
    id: string;
    title: string;
    description: string;
    votes: number;
    comments: number;
    commentsList: Comment[];
    status: 'Planned' | 'In Progress' | 'Complete' | 'Under Review' | 'Open';
    priority: 'Low' | 'Medium' | 'High';
    author: string;
    date: string;
}

export const requests: Request[] = [
    {
        id: '1',
        title: 'Dark Mode Support',
        description: 'It would be great to have a dark mode for late night coding sessions. The current bright white background is a bit too much for my eyes.',
        votes: 128,
        comments: 2,
        commentsList: [
            {id: 'c1', author: 'Sarah', avatar: 'S', content: 'Totally agree! My eyes are burning.', date: '1 day ago'},
            {id: 'c2', author: 'Mike', avatar: 'M', content: 'This is a must-have for 2024.', date: '2 hours ago'}
        ],
        status: 'In Progress',
        priority: 'High',
        author: 'AlexD',
        date: '2 days ago'
    },
    {
        id: '2',
        title: 'Export to CSV',
        description: 'Allow users to export their data to a CSV file for further analysis in Excel or Google Sheets.',
        votes: 85,
        comments: 1,
        commentsList: [
            {id: 'c3', author: 'DataAnalyst', avatar: 'D', content: 'JSON export would be nice too.', date: '5 days ago'}
        ],
        status: 'Planned',
        priority: 'Medium',
        author: 'SarahJ',
        date: '1 week ago'
    },
    {
        id: '3',
        title: 'Mobile App Notification',
        description: 'Push notifications for the mobile app when a new request is assigned to me.',
        votes: 256,
        comments: 0,
        commentsList: [],
        status: 'Under Review',
        priority: 'High',
        author: 'MikeT',
        date: '3 days ago'
    },
    {
        id: '4',
        title: 'API Integration for Slack',
        description: 'Direct integration with Slack to post new requests into a specific channel.',
        votes: 34,
        comments: 0,
        commentsList: [],
        status: 'Open',
        priority: 'Low',
        author: 'DevOpsGuy',
        date: '5 hours ago'
    },
    {
        id: '5',
        title: 'Custom Status Fields',
        description: 'We need the ability to define our own status fields for the requests workflow.',
        votes: 112,
        comments: 0,
        commentsList: [],
        status: 'Planned',
        priority: 'Medium',
        author: 'ManagerKim',
        date: '2 weeks ago'
    },
    {
        id: '6',
        title: 'Markdown Support in Comments',
        description: 'Please add support for Markdown in the comment section so we can share code snippets easily.',
        votes: 67,
        comments: 0,
        commentsList: [],
        status: 'Complete',
        priority: 'Low',
        author: 'Coder123',
        date: '1 month ago'
    },
    {
        id: '7',
        title: 'Multi-language Support',
        description: 'Add support for Spanish and French languages.',
        votes: 45,
        comments: 0,
        commentsList: [],
        status: 'Open',
        priority: 'Low',
        author: 'GlobalUser',
        date: '3 days ago'
    },
    {
        id: '8',
        title: 'Kanban Board View',
        description: 'A Kanban board view to visualize the progress of requests.',
        votes: 210,
        comments: 1,
        commentsList: [
            {id: 'c4', author: 'PM_Jane', avatar: 'P', content: 'This would help us visualize workflow much better.', date: '3 days ago'}
        ],
        status: 'In Progress',
        priority: 'High',
        author: 'AgileMaster',
        date: '1 week ago'
    }
];

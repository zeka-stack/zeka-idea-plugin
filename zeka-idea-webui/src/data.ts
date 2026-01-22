import type {Feedback, FeedbackComment} from './lib/api';

export type Comment = FeedbackComment;

export interface Request extends Feedback {
    commentsList?: Comment[];
}

export const requests: Request[] = [];

import React, {useState} from 'react';
import {X} from 'lucide-react';
import type {Request} from '../data';

interface NewRequestModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (request: Pick<Request, 'title' | 'description' | 'priority'>) => void;
}

export const NewRequestModal: React.FC<NewRequestModalProps> = ({isOpen, onClose, onSubmit}) => {
    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [priority, setPriority] = useState<'Low' | 'Medium' | 'High'>('Low');

    React.useEffect(() => {
        const handleEsc = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', handleEsc);
        return () => window.removeEventListener('keydown', handleEsc);
    }, [onClose]);

    if (!isOpen) return null;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim() || !description.trim() || title.length > 30 || description.length > 1000) return;

        onSubmit({
            title,
            description,
            priority
        });

        // Reset form
        setTitle('');
        setDescription('');
        setPriority('Low');
        onClose();
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/50 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-white rounded-xl shadow-xl w-full max-w-lg overflow-hidden animate-in zoom-in-95 duration-200">
                <div className="flex items-center justify-between p-4 border-b border-gray-100">
                    <h2 className="text-lg font-bold text-gray-900">Submit New Idea</h2>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-gray-500 hover:bg-gray-100 p-2 rounded-lg transition-colors"
                    >
                        <X className="w-5 h-5"/>
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-4 sm:p-6 space-y-4">
                    <div>
                        <div className="flex justify-between items-end mb-1">
                            <label htmlFor="title" className="block text-sm font-medium text-gray-700">
                                Title
                            </label>
                            <span className={`text-[10px] ${title.length > 30 ? 'text-red-500' : 'text-gray-400'}`}>
                                {title.length}/30
                            </span>
                        </div>
                        <input
                            type="text"
                            id="title"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            maxLength={30}
                            placeholder="What's your feature request?"
                            className="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all text-sm"
                            required
                        />
                    </div>

                    <div>
                        <div className="flex justify-between items-end mb-1">
                            <label htmlFor="description" className="block text-sm font-medium text-gray-700">
                                Description
                            </label>
                            <span className={`text-[10px] ${description.length > 1000 ? 'text-red-500' : 'text-gray-400'}`}>
                                {description.length}/1000
                            </span>
                        </div>
                        <textarea
                            id="description"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            maxLength={1000}
                            placeholder="Describe what you want to achieve..."
                            rows={4}
                            className="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all text-sm resize-none"
                            required
                        />
                    </div>

                    <div>
                        <label htmlFor="priority" className="block text-sm font-medium text-gray-700 mb-1">
                            Priority
                        </label>
                        <div className="grid grid-cols-3 gap-3">
                            {(['Low', 'Medium', 'High'] as const).map((p) => (
                                <button
                                    key={p}
                                    type="button"
                                    onClick={() => setPriority(p)}
                                    className={`py-2 px-3 text-sm font-medium rounded-lg border transition-all ${
                                        priority === p
                                            ? 'bg-indigo-50 border-indigo-200 text-indigo-700 shadow-sm'
                                            : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50 hover:border-gray-300'
                                    }`}
                                >
                                    {p}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="pt-2 flex gap-3">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 px-4 py-2.5 bg-white border border-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition-colors text-sm"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            className="flex-1 px-4 py-2.5 bg-indigo-600 text-white rounded-lg font-medium hover:bg-indigo-700 transition-colors shadow-sm text-sm"
                        >
                            Submit Idea
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

import type {Dispatch, SetStateAction} from 'react';
import {useCallback} from 'react';
import type {Attachment} from '../types.js';

interface CompletionLike {
  close: () => void;
}

export interface UseSubmitHandlerOptions {
  getTextContent: () => string;
  attachments: Attachment[];
  isLoading: boolean;
  clearInput: () => void;
  externalAttachments: Attachment[] | undefined;
  setInternalAttachments: Dispatch<SetStateAction<Attachment[]>>;
  fileCompletion: CompletionLike;
  recordInputHistory: (text: string) => void;
  onSubmit?: (content: string, attachmentsToSend?: Attachment[]) => void;
  addToast?: (message: string, type: 'info' | 'warning' | 'error' | 'success') => void;
  t: (key: string, options?: Record<string, unknown>) => string;
}

/**
 * useSubmitHandler - Submit logic for the chat input box
 *
 * - Validates SDK state and empty input
 * - Records input history
 * - Clears input/attachments for responsiveness
 * - Defers onSubmit to allow UI update
 */
export function useSubmitHandler({
  getTextContent,
  attachments,
  isLoading,
  clearInput,
  externalAttachments,
  setInternalAttachments,
  fileCompletion,
  recordInputHistory,
  onSubmit,
  addToast,
  t,
}: UseSubmitHandlerOptions) {
  return useCallback(() => {
    const content = getTextContent();
    const cleanContent = content.replace(/[\u200B-\u200D\uFEFF]/g, '').trim();

    if (!cleanContent && attachments.length === 0) return;
    if (isLoading) return;

    fileCompletion.close();

    recordInputHistory(content);

    const attachmentsToSend = attachments.length > 0 ? [...attachments] : undefined;

    clearInput();
    if (externalAttachments === undefined) {
      setInternalAttachments([]);
    }

    setTimeout(() => {
      onSubmit?.(content, attachmentsToSend);
    }, 10);
  }, [
    getTextContent,
    attachments,
    isLoading,
    clearInput,
    externalAttachments,
    setInternalAttachments,
    fileCompletion,
    recordInputHistory,
    onSubmit,
    addToast,
    t,
  ]);
}

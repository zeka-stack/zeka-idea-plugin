import type {Attachment} from './types.js';
import {AttachmentList} from './AttachmentList.js';
import {ContextBar} from './ContextBar.js';

export function ChatInputBoxHeader({
  attachments,
  onRemoveAttachment,
  activeFile,
  selectedLines,
  onClearContext,
  onAddAttachment,
}: {
  attachments: Attachment[];
  onRemoveAttachment: (id: string) => void;
  activeFile?: string;
  selectedLines?: string;
  onClearContext?: () => void;
  onAddAttachment: (files: FileList) => void;
}) {
  return (
    <>
      {/* Attachment list */}
      {attachments.length > 0 && (
        <AttachmentList attachments={attachments} onRemove={onRemoveAttachment} />
      )}

      {/* Context bar (Top Control Bar) */}
      <ContextBar
        activeFile={activeFile}
        selectedLines={selectedLines}
        onClearFile={onClearContext}
        onAddAttachment={onAddAttachment}
      />
    </>
  );
}

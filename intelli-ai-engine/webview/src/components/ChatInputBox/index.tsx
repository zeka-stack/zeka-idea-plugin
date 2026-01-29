/**
 * ChatInputBox 组件模块导出
 * 功能: 004-refactor-input-box
 */

export { ChatInputBox, default } from './ChatInputBox';
export { ButtonArea } from './ButtonArea';
export { AttachmentList } from './AttachmentList';
export { ModelSelect } from './selectors';

// 导出类型
export type {
  Attachment,
  ChatInputBoxHandle,
  ChatInputBoxProps,
  ButtonAreaProps,
  AttachmentListProps,
  DropdownItemData,
  DropdownPosition,
  TriggerQuery,
  FileItem,
  CompletionType,
} from './types';

// 导出常量
export { AVAILABLE_MODELS, IMAGE_MEDIA_TYPES, isImageAttachment } from './types';

import {useEffect, useMemo, useState} from 'react';
import {useTranslation} from 'react-i18next';
import type {HistoryData, HistorySessionSummary} from '../../types';
import VirtualList from './VirtualList';
import {Claude, OpenAI} from '@lobehub/icons';
import {extractCommandMessageContent} from '../../utils/messageUtils';

interface HistoryViewProps {
  historyData: HistoryData | null;
  currentProvider?: string; // 当前提供商 (claude 或 codex)
  onLoadSession: (sessionId: string) => void;
  onDeleteSession: (sessionId: string) => void; // 添加删除回调
  onExportSession: (sessionId: string, title: string) => void; // 添加导出回调
  onToggleFavorite: (sessionId: string) => void; // 添加收藏切换回调
  onUpdateTitle: (sessionId: string, newTitle: string) => void; // 添加标题更新回调
}

const formatTimeAgo = (timestamp: string | undefined, t: (key: string) => string) => {
  if (!timestamp) {
    return '';
  }
  const seconds = Math.floor((Date.now() - new Date(timestamp).getTime()) / 1000);
  const units: [number, string][] = [
    [31536000, t('history.timeAgo.yearsAgo')],
    [2592000, t('history.timeAgo.monthsAgo')],
    [86400, t('history.timeAgo.daysAgo')],
    [3600, t('history.timeAgo.hoursAgo')],
    [60, t('history.timeAgo.minutesAgo')],
  ];

  for (const [unitSeconds, label] of units) {
    const interval = Math.floor(seconds / unitSeconds);
    if (interval >= 1) {
      return `${interval} ${label}`;
    }
  }
  return `${Math.max(seconds, 1)} ${t('history.timeAgo.secondsAgo')}`;
};

const HistoryView = ({ historyData, currentProvider: _currentProvider, onLoadSession, onDeleteSession, onExportSession, onToggleFavorite, onUpdateTitle }: HistoryViewProps) => {
  const { t } = useTranslation();
  const [viewportHeight, setViewportHeight] = useState(() => window.innerHeight || 600);
  const [deletingSessionId, setDeletingSessionId] = useState<string | null>(null); // 记录待删除的会话ID
  const [inputValue, setInputValue] = useState(''); // 搜索输入框的即时值
  const [searchQuery, setSearchQuery] = useState(''); // 实际用于搜索的关键词（防抖后）
  const [editingSessionId, setEditingSessionId] = useState<string | null>(null); // 正在编辑的会话ID
  const [editingTitle, setEditingTitle] = useState(''); // 编辑中的标题内容

  useEffect(() => {
    const handleResize = () => setViewportHeight(window.innerHeight || 600);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // 防抖：输入完成 300ms 后更新搜索关键词
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearchQuery(inputValue);
    }, 300);

    return () => clearTimeout(timer);
  }, [inputValue]);

  // 对会话进行排序和搜索过滤：收藏的在上面（按收藏时间倒序），未收藏的在下面（保持原顺序）
  const sessions = useMemo(() => {
    const rawSessions = historyData?.sessions ?? [];

    // 搜索过滤（不区分大小写）
    const filteredSessions = searchQuery.trim()
      ? rawSessions.filter(s =>
          s.title?.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : rawSessions;

    // 分离收藏和未收藏的会话
    const favorited = filteredSessions.filter(s => s.isFavorited);
    const unfavorited = filteredSessions.filter(s => !s.isFavorited);

    // 收藏的会话按收藏时间倒序排序
    favorited.sort((a, b) => (b.favoritedAt || 0) - (a.favoritedAt || 0));

    // 合并：收藏的在前面，未收藏的在后面
    return [...favorited, ...unfavorited];
  }, [historyData?.sessions, searchQuery]);

  const infoBar = useMemo(() => {
    if (!historyData) {
      return '';
    }
    const sessionCount = sessions.length;
    const messageCount = historyData.total ?? 0;
    return t('history.totalSessions', { count: sessionCount, total: messageCount });
  }, [historyData, sessions.length, t]);

  if (!historyData) {
    return (
      <div className="messages-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ textAlign: 'center', color: '#858585' }}>
          <div style={{
            width: '48px',
            height: '48px',
            margin: '0 auto 16px',
            border: '4px solid rgba(133, 133, 133, 0.2)',
            borderTop: '4px solid #858585',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite'
          }}></div>
          <div>{t('history.loading')}</div>
        </div>
      </div>
    );
  }

  if (!historyData.success) {
    return (
      <div className="messages-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ textAlign: 'center', color: '#858585' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
          <div>{historyData.error ?? t('history.loadFailed')}</div>
        </div>
      </div>
    );
  }

  // 渲染空状态（搜索无结果或无会话）
  const renderEmptyState = () => {
    // 如果是搜索无结果
    if (searchQuery.trim() && sessions.length === 0) {
      return (
        <div className="messages-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
          <div style={{ textAlign: 'center', color: '#858585' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔍</div>
            <div>{t('history.noSearchResults')}</div>
            <div style={{ fontSize: '12px', marginTop: '8px' }}>尝试其他搜索关键词</div>
          </div>
        </div>
      );
    }

    // 如果完全没有会话
    if (!searchQuery.trim() && sessions.length === 0) {
      return (
        <div className="messages-container" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
          <div style={{ textAlign: 'center', color: '#858585' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>📭</div>
            <div>{t('history.noSessions')}</div>
            <div style={{ fontSize: '12px', marginTop: '8px' }}>{t('history.noSessionsDesc')}</div>
          </div>
        </div>
      );
    }

    return null;
  };

  // 处理删除按钮点击(阻止事件冒泡,避免触发会话加载)
  const handleDeleteClick = (e: React.MouseEvent, sessionId: string) => {
    e.stopPropagation(); // 阻止点击事件冒泡到父元素
    setDeletingSessionId(sessionId); // 显示确认对话框
  };

  // 处理导出按钮点击(阻止事件冒泡,避免触发会话加载)
  const handleExportClick = (e: React.MouseEvent, sessionId: string, title: string) => {
    e.stopPropagation(); // 阻止点击事件冒泡到父元素
    onExportSession(sessionId, title);
  };

  // 处理收藏按钮点击(阻止事件冒泡,避免触发会话加载)
  const handleFavoriteClick = (e: React.MouseEvent, sessionId: string) => {
    e.stopPropagation(); // 阻止点击事件冒泡到父元素
    onToggleFavorite(sessionId);
  };

  // 确认删除
  const confirmDelete = () => {
    if (deletingSessionId) {
      onDeleteSession(deletingSessionId);
      setDeletingSessionId(null);
    }
  };

  // 取消删除
  const cancelDelete = () => {
    setDeletingSessionId(null);
  };

  // 处理编辑按钮点击
  const handleEditClick = (e: React.MouseEvent, sessionId: string, currentTitle: string) => {
    e.stopPropagation(); // 阻止点击事件冒泡到父元素
    setEditingSessionId(sessionId);
    setEditingTitle(currentTitle);
  };

  // 保存编辑后的标题
  const handleSaveTitle = (e: React.MouseEvent, sessionId: string) => {
    e.stopPropagation();
    const trimmedTitle = editingTitle.trim();

    if (!trimmedTitle) {
      return; // 标题不能为空
    }

    if (trimmedTitle.length > 50) {
      // 超过50个字符，显示错误提示
      alert(t('history.titleTooLong'));
      return;
    }

    // 调用回调函数更新标题
    onUpdateTitle(sessionId, trimmedTitle);

    // 退出编辑模式
    setEditingSessionId(null);
    setEditingTitle('');
  };

  // 取消编辑
  const handleCancelEdit = (e: React.MouseEvent) => {
    e.stopPropagation();
    setEditingSessionId(null);
    setEditingTitle('');
  };

  // 高亮显示匹配的文本
  const highlightText = (text: string, query: string) => {
    if (!query.trim()) {
      return <span>{text}</span>;
    }

    const lowerText = text.toLowerCase();
    const lowerQuery = query.toLowerCase();
    const index = lowerText.indexOf(lowerQuery);

    if (index === -1) {
      return <span>{text}</span>;
    }

    const before = text.slice(0, index);
    const match = text.slice(index, index + query.length);
    const after = text.slice(index + query.length);

    return (
      <span>
        {before}
        <mark style={{ backgroundColor: '#ffd700', color: '#000', padding: '0 2px' }}>{match}</mark>
        {after}
      </span>
    );
  };

  const renderHistoryItem = (session: HistorySessionSummary) => {
    const isEditing = editingSessionId === session.sessionId;

    return (
      <div key={session.sessionId} className="history-item" onClick={() => !isEditing && onLoadSession(session.sessionId)}>
        <div className="history-item-header">
          <div className="history-item-title">
            {/* Provider Logo */}
            {session.provider && (
              <span
                className="history-provider-badge"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  marginRight: '8px',
                  verticalAlign: 'middle'
                }}
                title={session.provider === 'claude' ? 'Claude' : 'Codex'}
              >
                {session.provider === 'codex' ? (
                  <OpenAI.Avatar size={20} />
                ) : (
                  <Claude.Color size={20} />
                )}
              </span>
            )}
            {isEditing ? (
              // 编辑模式：显示输入框和保存/取消按钮
              <div className="history-title-edit-mode" onClick={(e) => e.stopPropagation()}>
                <input
                  type="text"
                  className="history-title-input"
                  value={editingTitle}
                  onChange={(e) => setEditingTitle(e.target.value)}
                  maxLength={50}
                  autoFocus
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      handleSaveTitle(e as any, session.sessionId);
                    } else if (e.key === 'Escape') {
                      handleCancelEdit(e as any);
                    }
                  }}
                />
                <button
                  className="history-title-save-btn"
                  onClick={(e) => handleSaveTitle(e, session.sessionId)}
                  title={t('history.saveTitleButton')}
                >
                  <span className="codicon codicon-check"></span>
                </button>
                <button
                  className="history-title-cancel-btn"
                  onClick={(e) => handleCancelEdit(e)}
                  title={t('history.cancelEditButton')}
                >
                  <span className="codicon codicon-close"></span>
                </button>
              </div>
            ) : (
              // 正常模式：显示标题（带高亮），提取 <command-message> 内容
              highlightText(extractCommandMessageContent(session.title), searchQuery)
            )}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div className="history-item-time">{formatTimeAgo(session.lastTimestamp, t)}</div>
            {!isEditing && (
              <>
                {/* 编辑按钮 */}
                <button
                  className="history-edit-btn"
                  onClick={(e) => handleEditClick(e, session.sessionId, session.title)}
                  title={t('history.editTitle')}
                  aria-label={t('history.editTitle')}
                >
                  <span className="codicon codicon-edit"></span>
                </button>
                {/* 收藏按钮 */}
                <button
                  className={`history-favorite-btn ${session.isFavorited ? 'favorited' : ''}`}
                  onClick={(e) => handleFavoriteClick(e, session.sessionId)}
                  title={session.isFavorited ? t('history.unfavoriteSession') : t('history.favoriteSession')}
                  aria-label={session.isFavorited ? t('history.unfavoriteSession') : t('history.favoriteSession')}
                >
                  <span className={session.isFavorited ? 'codicon codicon-star-full' : 'codicon codicon-star-empty'}></span>
                </button>
                {/* 导出按钮 */}
                <button
                  className="history-export-btn"
                  onClick={(e) => handleExportClick(e, session.sessionId, session.title)}
                  title={t('history.exportSession')}
                  aria-label={t('history.exportSession')}
                >
                  <span className="codicon codicon-arrow-down"></span>
                </button>
                {/* 删除按钮 */}
                <button
                  className="history-delete-btn"
                  onClick={(e) => handleDeleteClick(e, session.sessionId)}
                  title={t('history.deleteSession')}
                  aria-label={t('history.deleteSession')}
                >
                  <span className="codicon codicon-trash"></span>
                </button>
              </>
            )}
          </div>
        </div>
        <div className="history-item-meta">
          <span>{t('history.messageCount', { count: session.messageCount })}</span>
          <span style={{ fontFamily: 'var(--idea-editor-font-family, monospace)', color: '#666' }}>{session.sessionId.slice(0, 8)}</span>
        </div>
      </div>
    );
  };

  const listHeight = Math.max(240, viewportHeight - 118);

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div className="history-header">
        <div className="history-info">{infoBar}</div>
        {/* 搜索框 */}
        <div className="history-search-container">
          <input
            type="text"
            className="history-search-input"
            placeholder={t('history.searchPlaceholder')}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
          />
          <span
            className="codicon codicon-search history-search-icon"
          ></span>
        </div>
      </div>
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {sessions.length > 0 ? (
          <VirtualList
            items={sessions}
            itemHeight={78}
            height={listHeight}
            renderItem={renderHistoryItem}
            getItemKey={(session) => session.sessionId}
            className="messages-container"
          />
        ) : (
          renderEmptyState()
        )}
      </div>

      {/* 删除确认对话框 */}
      {deletingSessionId && (
        <div className="modal-overlay" onClick={cancelDelete}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h3>{t('history.confirmDelete')}</h3>
            <p>{t('history.deleteMessage')}</p>
            <div className="modal-actions">
              <button className="modal-btn modal-btn-cancel" onClick={cancelDelete}>
                {t('common.cancel')}
              </button>
              <button className="modal-btn modal-btn-danger" onClick={confirmDelete}>
                {t('common.delete')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default HistoryView;


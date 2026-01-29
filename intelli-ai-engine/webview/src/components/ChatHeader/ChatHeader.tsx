export interface ChatHeaderProps {
  sessionTitle: string;
}

export function ChatHeader({
  sessionTitle,
}: ChatHeaderProps): React.ReactElement | null {
  return (
    <div className="header">
      <div className="header-left">
        <div
          className="session-title"
          style={{
            fontWeight: 600,
            fontSize: '14px',
            paddingLeft: '8px',
          }}
        >
          {sessionTitle}
        </div>
      </div>
    </div>
  );
}

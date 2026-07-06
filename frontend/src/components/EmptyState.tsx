import { Database } from 'lucide-react';

interface EmptyStateProps {
  title?: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export default function EmptyState({
  title = 'データが見つかりません',
  description = '条件に該当するデータが存在しないか、まだ登録されていません。',
  actionLabel,
  onAction
}: EmptyStateProps) {
  return (
    <div className="empty-card-container fade-in">
      <Database size={48} className="empty-card-icon" aria-hidden="true" />
      <h3 className="empty-card-title">{title}</h3>
      <p className="empty-card-desc">{description}</p>
      {actionLabel && onAction && (
        <button 
          type="button"
          className="btn btn-secondary" 
          onClick={onAction}
          style={{ padding: '0.6rem 1.5rem', fontSize: '0.9rem' }}
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}

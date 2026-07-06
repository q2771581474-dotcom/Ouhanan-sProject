// InlineLoading skeleton placeholder

interface InlineLoadingProps {
  rows?: number;
}

export default function InlineLoading({ rows = 5 }: InlineLoadingProps) {
  return (
    <div 
      className="fade-in" 
      style={{ width: '100%', padding: '1rem' }} 
      aria-busy="true" 
      aria-live="polite"
    >
      {Array.from({ length: rows }).map((_, idx) => (
        <div key={idx} className="skeleton-row">
          <div className="skeleton-box" style={{ height: '2rem', flex: '1' }}></div>
          <div className="skeleton-box" style={{ height: '2rem', flex: '2' }}></div>
          <div className="skeleton-box" style={{ height: '2rem', flex: '1' }}></div>
          <div className="skeleton-box" style={{ height: '2rem', flex: '1' }}></div>
          <div className="skeleton-box" style={{ height: '2rem', flex: '1' }}></div>
        </div>
      ))}
    </div>
  );
}

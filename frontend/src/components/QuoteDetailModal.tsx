import { QuoteResponseData } from '../types';

interface QuoteDetailModalProps {
  quote: QuoteResponseData | null;
  onClose: () => void;
}

export default function QuoteDetailModal({ quote, onClose }: QuoteDetailModalProps) {
  if (!quote) return null;

  return (
    <div 
      className="modal-backdrop" 
      onClick={onClose} 
      role="dialog" 
      aria-modal="true" 
      aria-labelledby="detail-modal-title"
    >
      <div 
        className="modal-content-card fade-in" 
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h2 id="detail-modal-title" style={{ margin: 0 }}>見積詳細情報: {quote.quoteNo}</h2>
          <button 
            type="button"
            className="btn btn-secondary modal-close-btn" 
            onClick={onClose}
          >
            閉じる
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
          <div className="info-row">
            <span className="info-label">運転者年齢:</span>
            <span className="info-value">{quote.driverAge} 歳</span>
          </div>
          <div className="info-row">
            <span className="info-label">免許証の色:</span>
            <span className="info-value">{quote.licenseColor}</span>
          </div>
          <div className="info-row">
            <span className="info-label">使用目的:</span>
            <span className="info-value">
              {quote.usageType === 'PRIVATE' ? '日常・レジャー' : quote.usageType === 'COMMUTE' ? '通勤・通学' : '業務使用'}
            </span>
          </div>
          <div className="info-row">
            <span className="info-label">年間走行距離:</span>
            <span className="info-value">{quote.annualMileage.toLocaleString()} km</span>
          </div>
          <div className="info-row">
            <span className="info-label">運転者範囲:</span>
            <span className="info-value">
              {quote.driverRange === 'SELF' ? '本人限定' : quote.driverRange === 'COUPLE' ? '本人・配偶者限定' : quote.driverRange === 'FAMILY' ? '同居の親族限定' : '限定なし'}
            </span>
          </div>
          <div className="info-row">
            <span className="info-label">他社保険加入状況:</span>
            <span className="info-value">
              {quote.hasCurrentInsurance ? `${quote.grade}等級 (事故有${quote.accidentTerm}年)` : '未加入'}
            </span>
          </div>
          <div className="info-row">
            <span className="info-label">メーカー/車名:</span>
            <span className="info-value">{quote.maker} {quote.carName}</span>
          </div>
          <div className="info-row">
            <span className="info-label">初度登録年月:</span>
            <span className="info-value">{quote.firstRegistrationYearMonth}</span>
          </div>
          <div className="info-row">
            <span className="info-label">車両タイプ:</span>
            <span className="info-value">
              {quote.vehicleType === 'KEI' ? '軽自動車' : quote.vehicleType === 'COMPACT' ? '小型車' : quote.vehicleType === 'SEDAN' ? '普通セダン' : quote.vehicleType === 'MINIVAN' ? 'ミニバン' : 'SUV'}
            </span>
          </div>
          <div className="info-row">
            <span className="info-label">車両保険付帯:</span>
            <span className="info-value">{quote.vehicleInsurance ? 'あり' : 'なし'}</span>
          </div>
        </div>

        <div style={{ background: 'rgba(0,0,0,0.2)', padding: '1.25rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)', marginBottom: '1.5rem', display: 'flex', justifyContent: 'space-around' }}>
          <div>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>年間保険料</div>
            <strong style={{ fontSize: '1.5rem', color: 'var(--secondary)' }}>¥ {quote.annualPremium.toLocaleString()} 円</strong>
          </div>
          <div>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>月額目安</div>
            <strong style={{ fontSize: '1.5rem', color: 'var(--text-primary)' }}>¥ {quote.monthlyPremium.toLocaleString()} 円</strong>
          </div>
        </div>

        <h3>計算の適用内訳</h3>
        <div className="table-container" style={{ marginTop: '0.75rem' }}>
          <table>
            <thead>
              <tr>
                <th>適用項目</th>
                <th style={{ textAlign: 'right' }}>料率 / 加算額</th>
              </tr>
            </thead>
            <tbody>
              {quote.breakdowns.map((b, i) => (
                <tr key={i}>
                  <td>{b.itemName}</td>
                  <td style={{ textAlign: 'right', fontWeight: 600 }}>
                    {b.amount && b.amount > 0 ? `+ ${b.amount.toLocaleString()} 円` : `× ${b.rate?.toFixed(2)}`}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { 
  CheckCircle, ArrowRight, ArrowLeft, 
  AlertCircle, Search, Download, 
  LogOut, Lock
} from 'lucide-react';
import { QuoteData, QuoteResponseData } from './types';

// 子コンポーネントインポート
import Stepper from './components/Stepper';
import EmptyState from './components/EmptyState';
import InlineLoading from './components/InlineLoading';
import QuoteDetailModal from './components/QuoteDetailModal';

const API_BASE = window.location.port === '5173' ? 'http://localhost:8080' : '';
type LicenseColor = Exclude<QuoteData['licenseColor'], ''>;
type UsageType = Exclude<QuoteData['usageType'], ''>;
type DriverRange = Exclude<QuoteData['driverRange'], ''>;
type VehicleType = Exclude<QuoteData['vehicleType'], ''>;

const initialQuoteData: QuoteData = {
  driverAge: '' as unknown as number,
  licenseColor: '',
  usageType: '',
  annualMileage: '' as unknown as number,
  driverRange: '',
  hasCurrentInsurance: '' as unknown as boolean,
  grade: '',
  accidentTerm: '',
  maker: '',
  carName: '',
  firstRegistrationYearMonth: '',
  vehicleType: '',
  vehicleInsurance: '' as unknown as boolean,
  propertyDamageLimit: '',
  personalInjuryAmount: '',
  lawyerOption: '' as unknown as boolean,
  roadService: '' as unknown as boolean
};

export default function App() {
  const [view, setView] = useState<'USER_FLOW' | 'ADMIN_LOGIN' | 'ADMIN_DASHBOARD'>('USER_FLOW');
  const [step, setStep] = useState<number>(1);
  const [quoteData, setQuoteDataRaw] = useState<QuoteData>(initialQuoteData);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const setQuoteData = (updater: QuoteData | ((prev: QuoteData) => QuoteData)) => {
    const next = typeof updater === 'function' ? updater(quoteData) : updater;
    setQuoteDataRaw(next);
    if (Object.keys(errors).length > 0) {
      const newErrs = { ...errors };
      let changed = false;
      Object.keys(next).forEach(key => {
        if (next[key as keyof QuoteData] !== quoteData[key as keyof QuoteData] && newErrs[key]) {
          delete newErrs[key];
          changed = true;
        }
      });
      if (changed) {
        setErrors(newErrs);
      }
    }
  };
  const [quoteResult, setQuoteResult] = useState<QuoteResponseData | null>(null);
  
  // 管理画面用状態
  const [token, setToken] = useState<string>(localStorage.getItem('adminToken') || '');
  const [username, setUsername] = useState<string>(localStorage.getItem('adminUsername') || '');
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [loginError, setLoginError] = useState<string>('');
  
  // 管理者見積一覧検索用状態
  const [searchParams, setSearchParams] = useState({
    quoteNo: '',
    maker: '',
    carName: '',
    minAge: '',
    maxAge: ''
  });
  const [quotesList, setQuotesList] = useState<QuoteResponseData[]>([]);
  const [selectedQuoteDetail, setSelectedQuoteDetail] = useState<QuoteResponseData | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // 初回マウント時などの処理
  }, []);

  const handleLogoClick = () => {
    setView('USER_FLOW');
    setStep(1);
    setQuoteResult(null);
    setQuoteData(initialQuoteData);
    setErrors({});
  };

  const handleAdminLogout = () => {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminUsername');
    setToken('');
    setUsername('');
    setView('USER_FLOW');
    setStep(1);
  };

  // キーボード操作補助
  const handleKeyDown = (e: React.KeyboardEvent, action: () => void) => {
    if (e.key === ' ' || e.key === 'Enter') {
      e.preventDefault();
      action();
    }
  };

  // --- バリデーションチェック群 ---
  const validateStep2 = (): boolean => {
    const errs: Record<string, string> = {};
    if (quoteData.driverAge === undefined || quoteData.driverAge === null || quoteData.driverAge === '' as any || isNaN(quoteData.driverAge)) {
      errs.driverAge = '運転者年齢は必須項目です。';
    } else if (quoteData.driverAge < 18 || quoteData.driverAge > 100) {
      errs.driverAge = '運転者年齢は18歳以上100歳以下で入力してください。';
    }

    if (!quoteData.licenseColor) errs.licenseColor = '免許証の色を選択してください。';
    if (!quoteData.usageType) errs.usageType = '使用目的を選択してください。';
    
    if (quoteData.annualMileage === undefined || quoteData.annualMileage === null || quoteData.annualMileage === '' as any || isNaN(quoteData.annualMileage)) {
      errs.annualMileage = '年間走行距離は必須項目です。';
    } else if (quoteData.annualMileage < 0 || quoteData.annualMileage > 30000) {
      errs.annualMileage = '年間走行距離は0km〜30,000kmの範囲で入力してください。';
    }

    if (!quoteData.driverRange) errs.driverRange = '運転者の範囲を選択してください。';

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const validateStep3 = (): boolean => {
    const errs: Record<string, string> = {};
    if (quoteData.hasCurrentInsurance === '' as any || quoteData.hasCurrentInsurance === undefined || quoteData.hasCurrentInsurance === null) {
      errs.hasCurrentInsurance = '他社の自動車保険に加入しているか選択してください。';
    } else if (quoteData.hasCurrentInsurance === true) {
      if (quoteData.grade === '' || quoteData.grade === undefined || quoteData.grade === null) {
        errs.grade = '現在の等級を選択してください。';
      } else if (Number(quoteData.grade) < 1 || Number(quoteData.grade) > 20) {
        errs.grade = '等級は1〜20の間で指定してください。';
      }

      if (quoteData.accidentTerm === '' || quoteData.accidentTerm === undefined || quoteData.accidentTerm === null) {
        errs.accidentTerm = '事故有係数適用期間を選択してください。';
      } else if (Number(quoteData.accidentTerm) < 0 || Number(quoteData.accidentTerm) > 6) {
        errs.accidentTerm = '事故有期間は0〜6年の間で指定してください。';
      }
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const validateStep4 = (): boolean => {
    const errs: Record<string, string> = {};
    if (!quoteData.maker || !quoteData.maker.trim()) errs.maker = 'メーカーを入力してください。';
    else if (quoteData.maker.length > 50) errs.maker = 'メーカー名は50文字以内で入力してください。';
    
    if (!quoteData.carName || !quoteData.carName.trim()) errs.carName = '車名を入力してください。';
    else if (quoteData.carName.length > 50) errs.carName = '車名は50文字以内で入力してください。';

    if (!quoteData.firstRegistrationYearMonth) {
      errs.firstRegistrationYearMonth = '初度登録年月を入力してください。';
    } else if (!/^\d{4}-\d{2}$/.test(quoteData.firstRegistrationYearMonth)) {
      errs.firstRegistrationYearMonth = '初度登録年月はYYYY-MM形式で入力してください。';
    } else {
      const [year, month] = quoteData.firstRegistrationYearMonth.split('-').map(Number);
      const inputDate = new Date(year, month - 1, 1);
      const currentDate = new Date();
      currentDate.setDate(1);
      currentDate.setHours(0,0,0,0);
      
      if (inputDate > currentDate) {
        errs.firstRegistrationYearMonth = '初度登録年月は現在より未来の年月を指定できません。';
      }
    }

    if (!quoteData.vehicleType) errs.vehicleType = '車両タイプを選択してください。';
    if (quoteData.vehicleInsurance === '' as any || quoteData.vehicleInsurance === undefined || quoteData.vehicleInsurance === null) {
      errs.vehicleInsurance = '車両保険を付帯するか選択してください。';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const validateStep5 = (): boolean => {
    const errs: Record<string, string> = {};
    if (!quoteData.propertyDamageLimit) errs.propertyDamageLimit = '対物補償の限度額を選択してください。';
    if (!quoteData.personalInjuryAmount) errs.personalInjuryAmount = '人身傷害の保険金額を選択してください。';
    if (quoteData.lawyerOption === '' as any || quoteData.lawyerOption === undefined || quoteData.lawyerOption === null) {
      errs.lawyerOption = '弁護士費用特約を付帯するか選択してください。';
    }
    if (quoteData.roadService === '' as any || quoteData.roadService === undefined || quoteData.roadService === null) {
      errs.roadService = 'ロードサービスを付帯するか選択してください。';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleNext = () => {
    if (step === 2 && !validateStep2()) return;
    if (step === 3 && !validateStep3()) return;
    if (step === 4 && !validateStep4()) return;
    if (step === 5 && !validateStep5()) return;

    setErrors({});
    setStep(prev => prev + 1);
  };

  const handleBack = () => {
    setErrors({});
    setStep(prev => prev - 1);
  };

  const handleSubmitQuote = async () => {
    setLoading(true);
    setErrors({});
    try {
      const payload = {
        ...quoteData,
        grade: quoteData.hasCurrentInsurance ? Number(quoteData.grade) : null,
        accidentTerm: quoteData.hasCurrentInsurance ? Number(quoteData.accidentTerm) : null,
        driverAge: Number(quoteData.driverAge),
        annualMileage: Number(quoteData.annualMileage)
      };

      const res = await axios.post(`${API_BASE}/api/quotes`, payload);
      setQuoteResult(res.data);
      setStep(7);
    } catch (err: unknown) {
      if (axios.isAxiosError<{ details?: Record<string, string> }>(err) && err.response?.data?.details) {
        setErrors(err.response.data.details);
      } else {
        alert('見積作成中にエラーが発生しました。入力項目をご確認ください。');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleAdminLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError('');
    setLoading(true);
    try {
      const res = await axios.post(`${API_BASE}/api/admin/login`, loginForm);
      const { token, username } = res.data;
      localStorage.setItem('adminToken', token);
      localStorage.setItem('adminUsername', username);
      setToken(token);
      setUsername(username);
      setView('ADMIN_DASHBOARD');
      fetchQuotesList(token);
    } catch {
      setLoginError('ログインIDまたはパスワードが正しくありません。');
    } finally {
      setLoading(false);
    }
  };

  const fetchQuotesList = async (authToken = token) => {
    setLoading(true);
    try {
      const params = {
        quoteNo: searchParams.quoteNo || null,
        maker: searchParams.maker || null,
        carName: searchParams.carName || null,
        minAge: searchParams.minAge ? Number(searchParams.minAge) : null,
        maxAge: searchParams.maxAge ? Number(searchParams.maxAge) : null
      };

      const res = await axios.get(`${API_BASE}/api/admin/quotes`, {
        headers: { Authorization: `Bearer ${authToken}` },
        params
      });
      setQuotesList(res.data);
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        handleAdminLogout();
      } else {
        alert('見積一覧の取得に失敗しました。');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadCsv = () => {
    const params = new URLSearchParams();
    if (searchParams.quoteNo) params.append('quoteNo', searchParams.quoteNo);
    if (searchParams.maker) params.append('maker', searchParams.maker);
    if (searchParams.carName) params.append('carName', searchParams.carName);
    if (searchParams.minAge) params.append('minAge', searchParams.minAge);
    if (searchParams.maxAge) params.append('maxAge', searchParams.maxAge);

    const downloadUrl = `${API_BASE}/api/admin/quotes.csv?${params.toString()}`;
    
    setLoading(true);
    axios.get(downloadUrl, {
      headers: { Authorization: `Bearer ${token}` },
      responseType: 'blob'
    }).then((res) => {
      const disposition = res.headers['content-disposition'];
      let filename = `quotes_${new Date().toISOString().slice(0,10)}.csv`;
      if (disposition && disposition.indexOf('filename=') !== -1) {
        filename = disposition.split('filename=')[1].trim();
      }
      
      const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }).catch(() => {
      alert('CSVエクスポートに失敗しました。');
    }).finally(() => {
      setLoading(false);
    });
  };

  return (
    <div className="app-container">
      {/* 共通ヘッダー */}
      <header className="header">
        <div className="logo" onClick={handleLogoClick}>
          <span>自動車保険見積</span>
        </div>
        <div>
          {view === 'USER_FLOW' ? (
            <button 
              type="button"
              className="btn btn-secondary" 
              style={{ padding: '0.5rem 1rem', fontSize: '0.85rem' }}
              onClick={() => {
                if (token) {
                  setView('ADMIN_DASHBOARD');
                  fetchQuotesList();
                } else {
                  setView('ADMIN_LOGIN');
                }
              }}
            >
              管理者メニュー
            </button>
          ) : (
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>ログイン中: {username}</span>
              <button 
                type="button"
                className="btn btn-secondary" 
                style={{ padding: '0.5rem 1rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                onClick={handleAdminLogout}
              >
                <LogOut size={16} /> ログアウト
              </button>
            </div>
          )}
        </div>
      </header>

      {/* メインコンテンツ */}
      <main className="main-content">
        {loading && view !== 'ADMIN_DASHBOARD' && (
          <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(15, 23, 42, 0.45)', backdropFilter: 'blur(4px)',
            display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
          }}>
            <div style={{ fontSize: '1.25rem', fontWeight: 600, color: 'var(--primary)' }}>
              処理中...
            </div>
          </div>
        )}

        {/* ==================== 1. 利用者向導フロー ==================== */}
        {view === 'USER_FLOW' && (
          <div className="fade-in">
            <Stepper step={step} />

            {/* SC-001: ホーム画面 */}
            {step === 1 && (
              <div className="fade-in">
                <div className="hero-section simple-hero">
                  <h1 className="hero-title">自動車保険 見積サイト</h1>
                  <p className="subtitle hero-copy">
                    本サイトは採用課題用の簡易見積アプリケーションです。入力条件に基づき、年度保険料・月額保険料・計算明細を確認できます。
                  </p>
                  <div className="scope-list" aria-label="本サイトでできること">
                    <span>見積条件入力</span>
                    <span>保険料計算</span>
                    <span>見積結果表示</span>
                  </div>
                  <p className="notice-text">
                    実際の契約申込、本人確認、決済、外部保険会社API連携は対象外です。
                  </p>
                  <button
                    type="button"
                    className="btn btn-primary hero-action"
                    onClick={() => setStep(2)}
                  >
                    見積を開始する <ArrowRight size={22} />
                  </button>
                </div>
              </div>
            )}

            {/* SC-002: 使用者情報 */}
            {step === 2 && (
              <div className="card">
                <h1>使用者情報の入力</h1>
                <p className="subtitle">運転される方の情報を入力してください。</p>

                <div className="form-group">
                  <label className="form-label">運転者年齢 <span className="required">*</span></label>
                  <input 
                    type="number" 
                    className={`form-control ${errors.driverAge ? 'input-error' : ''}`}
                    placeholder="例: 35"
                    value={quoteData.driverAge === '' as any ? '' : quoteData.driverAge}
                    onChange={(e) => setQuoteData({ ...quoteData, driverAge: e.target.value === '' ? '' as any : parseInt(e.target.value) })}
                  />
                  {errors.driverAge && <div className="error-message"><AlertCircle size={14}/>{errors.driverAge}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">免許証の色 <span className="required">*</span></label>
                  <div className="choice-grid" role="radiogroup" aria-label="免許証の色">
                    {[
                      { code: 'GOLD', label: 'ゴールド' },
                      { code: 'BLUE', label: 'ブルー' },
                      { code: 'GREEN', label: 'グリーン' }
                    ].map((item) => {
                      const isSelected = quoteData.licenseColor === item.code;
                      return (
                        <div 
                          key={item.code}
                          className={`choice-card ${isSelected ? 'selected' : ''} ${errors.licenseColor ? 'error-card' : ''}`}
                          onClick={() => setQuoteData({ ...quoteData, licenseColor: item.code as LicenseColor })}
                          tabIndex={0}
                          role="radio"
                          aria-checked={isSelected}
                          onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, licenseColor: item.code as LicenseColor }))}
                        >
                          {item.label}
                        </div>
                      );
                    })}
                  </div>
                  {errors.licenseColor && <div className="error-message"><AlertCircle size={14}/>{errors.licenseColor}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">使用目的 <span className="required">*</span></label>
                  <div className="choice-grid" role="radiogroup" aria-label="使用目的">
                    {[
                      { code: 'PRIVATE', label: '日常・レジャー' },
                      { code: 'COMMUTE', label: '通勤・通学' },
                      { code: 'BUSINESS', label: '業務使用' }
                    ].map((item) => {
                      const isSelected = quoteData.usageType === item.code;
                      return (
                        <div 
                          key={item.code}
                          className={`choice-card ${isSelected ? 'selected' : ''} ${errors.usageType ? 'error-card' : ''}`}
                          onClick={() => setQuoteData({ ...quoteData, usageType: item.code as UsageType })}
                          tabIndex={0}
                          role="radio"
                          aria-checked={isSelected}
                          onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, usageType: item.code as UsageType }))}
                        >
                          {item.label}
                        </div>
                      );
                    })}
                  </div>
                  {errors.usageType && <div className="error-message"><AlertCircle size={14}/>{errors.usageType}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">年間走行距離 <span className="required">*</span></label>
                  <div className="choice-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }} role="radiogroup" aria-label="年間走行距離">
                    {[
                      { val: 4000, label: '5,000km以下' },
                      { val: 8000, label: '5,001〜10,000km' },
                      { val: 15000, label: '10,001km以上' }
                    ].map((item) => {
                      const isSelected = quoteData.annualMileage === item.val;
                      return (
                        <div 
                          key={item.val}
                          className={`choice-card ${isSelected ? 'selected' : ''} ${errors.annualMileage ? 'error-card' : ''}`}
                          onClick={() => setQuoteData({ ...quoteData, annualMileage: item.val })}
                          tabIndex={0}
                          role="radio"
                          aria-checked={isSelected}
                          onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, annualMileage: item.val }))}
                        >
                          {item.label}
                        </div>
                      );
                    })}
                  </div>
                  {errors.annualMileage && <div className="error-message"><AlertCircle size={14}/>{errors.annualMileage}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">運転者範囲 <span className="required">*</span></label>
                  <div className="choice-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }} role="radiogroup" aria-label="運転者範囲">
                    {[
                      { code: 'SELF', label: '本人限定' },
                      { code: 'COUPLE', label: '本人・配偶者限定' },
                      { code: 'FAMILY', label: '同居の親族限定' },
                      { code: 'ANYONE', label: '限定なし' }
                    ].map((item) => {
                      const isSelected = quoteData.driverRange === item.code;
                      return (
                        <div 
                          key={item.code}
                          className={`choice-card ${isSelected ? 'selected' : ''} ${errors.driverRange ? 'error-card' : ''}`}
                          onClick={() => setQuoteData({ ...quoteData, driverRange: item.code as DriverRange })}
                          tabIndex={0}
                          role="radio"
                          aria-checked={isSelected}
                          onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, driverRange: item.code as DriverRange }))}
                        >
                          {item.label}
                        </div>
                      );
                    })}
                  </div>
                  {errors.driverRange && <div className="error-message"><AlertCircle size={14}/>{errors.driverRange}</div>}
                </div>

                <div className="btn-group">
                  <button type="button" className="btn btn-secondary" onClick={() => setStep(1)}><ArrowLeft size={18}/> 戻る</button>
                  <button type="button" className="btn btn-primary" onClick={handleNext}>次へ進む <ArrowRight size={18}/></button>
                </div>
              </div>
            )}

            {/* SC-003: 契約中保険 */}
            {step === 3 && (
              <div className="card">
                <h1>現在加入中の自動車保険について</h1>
                <p className="subtitle">現在の保険の有無により、適用等級などの割引率が変わります。</p>

                <div className="form-group">
                  <label className="form-label">現在、他社の自動車保険に加入していますか？ <span className="required">*</span></label>
                  <div className="choice-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }} role="radiogroup" aria-label="他社自動車保険加入状況">
                    <div 
                      className={`choice-card ${quoteData.hasCurrentInsurance === true ? 'selected' : ''} ${errors.hasCurrentInsurance ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, hasCurrentInsurance: true })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.hasCurrentInsurance === true}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, hasCurrentInsurance: true }))}
                    >
                      はい（現在加入している）
                    </div>
                    <div 
                      className={`choice-card ${quoteData.hasCurrentInsurance === false ? 'selected' : ''} ${errors.hasCurrentInsurance ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, hasCurrentInsurance: false, grade: '', accidentTerm: '' })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.hasCurrentInsurance === false}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, hasCurrentInsurance: false, grade: '', accidentTerm: '' }))}
                    >
                      いいえ（初めての加入など）
                    </div>
                  </div>
                  {errors.hasCurrentInsurance && <div className="error-message"><AlertCircle size={14}/>{errors.hasCurrentInsurance}</div>}
                </div>

                {quoteData.hasCurrentInsurance && (
                  <div className="fade-in" style={{ borderTop: '1px solid var(--border)', paddingTop: '1.5rem' }}>
                    <div className="form-group">
                      <label className="form-label">現在の等級 <span className="required">*</span></label>
                      <select 
                        className={`form-control ${errors.grade ? 'input-error' : ''}`}
                        value={quoteData.grade} 
                        onChange={(e) => setQuoteData({ ...quoteData, grade: e.target.value ? Number(e.target.value) : '' })}
                      >
                        <option value="">選択してください</option>
                        {Array.from({ length: 20 }, (_, i) => 20 - i).map((num) => (
                          <option key={num} value={num}>{num}等級</option>
                        ))}
                      </select>
                      {errors.grade && <div className="error-message"><AlertCircle size={14}/>{errors.grade}</div>}
                    </div>

                    <div className="form-group">
                      <label className="form-label">事故有係数適用期間 <span className="required">*</span></label>
                      <div className="choice-grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }} role="radiogroup" aria-label="事故有係数適用期間">
                        {[0, 1, 2, 3, 4, 5, 6].map((num) => {
                          const isSelected = quoteData.accidentTerm === num;
                          return (
                            <div 
                              key={num}
                              className={`choice-card ${isSelected ? 'selected' : ''} ${errors.accidentTerm ? 'error-card' : ''}`}
                              onClick={() => setQuoteData({ ...quoteData, accidentTerm: num })}
                              tabIndex={0}
                              role="radio"
                              aria-checked={isSelected}
                              onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, accidentTerm: num }))}
                            >
                              {num} 年
                            </div>
                          );
                        })}
                      </div>
                      {errors.accidentTerm && <div className="error-message"><AlertCircle size={14}/>{errors.accidentTerm}</div>}
                    </div>
                  </div>
                )}

                <div className="btn-group">
                  <button type="button" className="btn btn-secondary" onClick={handleBack}><ArrowLeft size={18}/> 戻る</button>
                  <button type="button" className="btn btn-primary" onClick={handleNext}>次へ進む <ArrowRight size={18}/></button>
                </div>
              </div>
            )}

            {/* SC-004: 車両情報 */}
            {step === 4 && (
              <div className="card">
                <h1>お車の情報</h1>
                <p className="subtitle">見積り対象の車両の情報を入力してください。</p>

                <div className="form-group">
                  <label className="form-label">メーカー <span className="required">*</span></label>
                  <input 
                    type="text" 
                    className={`form-control ${errors.maker ? 'input-error' : ''}`}
                    placeholder="例: トヨタ"
                    value={quoteData.maker}
                    onChange={(e) => setQuoteData({ ...quoteData, maker: e.target.value })}
                  />
                  {errors.maker && <div className="error-message"><AlertCircle size={14}/>{errors.maker}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">車名 <span className="required">*</span></label>
                  <input 
                    type="text" 
                    className={`form-control ${errors.carName ? 'input-error' : ''}`}
                    placeholder="例: プリウス"
                    value={quoteData.carName}
                    onChange={(e) => setQuoteData({ ...quoteData, carName: e.target.value })}
                  />
                  {errors.carName && <div className="error-message"><AlertCircle size={14}/>{errors.carName}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">初度登録年月 <span className="required">*</span></label>
                  <input 
                    type="month" 
                    className={`form-control ${errors.firstRegistrationYearMonth ? 'input-error' : ''}`}
                    value={quoteData.firstRegistrationYearMonth}
                    onChange={(e) => setQuoteData({ ...quoteData, firstRegistrationYearMonth: e.target.value })}
                  />
                  {errors.firstRegistrationYearMonth && <div className="error-message"><AlertCircle size={14}/>{errors.firstRegistrationYearMonth}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">車両タイプ <span className="required">*</span></label>
                  <div className="choice-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }} role="radiogroup" aria-label="車両タイプ">
                    {[
                      { code: 'KEI', label: '軽自動車' },
                      { code: 'COMPACT', label: '小型車' },
                      { code: 'SEDAN', label: '普通セダン' },
                      { code: 'MINIVAN', label: 'ミニバン' },
                      { code: 'SUV', label: 'SUV' }
                    ].map((item) => {
                      const isSelected = quoteData.vehicleType === item.code;
                      return (
                        <div 
                          key={item.code}
                          className={`choice-card ${isSelected ? 'selected' : ''} ${errors.vehicleType ? 'error-card' : ''}`}
                          onClick={() => setQuoteData({ ...quoteData, vehicleType: item.code as VehicleType })}
                          tabIndex={0}
                          role="radio"
                          aria-checked={isSelected}
                          onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, vehicleType: item.code as VehicleType }))}
                        >
                          {item.label}
                        </div>
                      );
                    })}
                  </div>
                  {errors.vehicleType && <div className="error-message"><AlertCircle size={14}/>{errors.vehicleType}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">車両保険を付帯しますか？ <span className="required">*</span></label>
                  <div className="choice-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }} role="radiogroup" aria-label="車両保険付帯状況">
                    <div 
                      className={`choice-card ${quoteData.vehicleInsurance === true ? 'selected' : ''} ${errors.vehicleInsurance ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, vehicleInsurance: true })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.vehicleInsurance === true}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, vehicleInsurance: true }))}
                    >
                      付帯する（+30,000円）
                    </div>
                    <div 
                      className={`choice-card ${quoteData.vehicleInsurance === false ? 'selected' : ''} ${errors.vehicleInsurance ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, vehicleInsurance: false })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.vehicleInsurance === false}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, vehicleInsurance: false }))}
                    >
                      付帯しない
                    </div>
                  </div>
                  {errors.vehicleInsurance && <div className="error-message"><AlertCircle size={14}/>{errors.vehicleInsurance}</div>}
                </div>

                <div className="btn-group">
                  <button type="button" className="btn btn-secondary" onClick={handleBack}><ArrowLeft size={18}/> 戻る</button>
                  <button type="button" className="btn btn-primary" onClick={handleNext}>次へ進む <ArrowRight size={18}/></button>
                </div>
              </div>
            )}

            {/* SC-005: 補償条件 */}
            {step === 5 && (
              <div className="card">
                <h1>補償条件と特約の設定</h1>
                <p className="subtitle">事故の際の補償内容やサービス特約を選択してください。</p>

                <div className="form-group">
                  <label className="form-label">対人賠償限度額</label>
                  <input type="text" className="form-control" value="無制限 固定" disabled />
                </div>

                <div className="form-group">
                  <label className="form-label">対物賠償限度額 <span className="required">*</span></label>
                  <div className="choice-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }} role="radiogroup" aria-label="対物賠償限度額">
                    <div 
                      className={`choice-card ${quoteData.propertyDamageLimit === 'UNLIMITED' ? 'selected' : ''} ${errors.propertyDamageLimit ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, propertyDamageLimit: 'UNLIMITED' })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.propertyDamageLimit === 'UNLIMITED'}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, propertyDamageLimit: 'UNLIMITED' }))}
                    >
                      無制限（+5,000円）
                    </div>
                    <div 
                      className={`choice-card ${quoteData.propertyDamageLimit === 'THIRTY_MILLION' ? 'selected' : ''} ${errors.propertyDamageLimit ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, propertyDamageLimit: 'THIRTY_MILLION' })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.propertyDamageLimit === 'THIRTY_MILLION'}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, propertyDamageLimit: 'THIRTY_MILLION' }))}
                    >
                      3,000万円
                    </div>
                  </div>
                  {errors.propertyDamageLimit && <div className="error-message"><AlertCircle size={14}/>{errors.propertyDamageLimit}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">人身傷害補償額 <span className="required">*</span></label>
                  <div className="choice-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }} role="radiogroup" aria-label="人身傷害補償額">
                    <div 
                      className={`choice-card ${quoteData.personalInjuryAmount === 'THIRTY_MILLION' ? 'selected' : ''} ${errors.personalInjuryAmount ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, personalInjuryAmount: 'THIRTY_MILLION' })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.personalInjuryAmount === 'THIRTY_MILLION'}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, personalInjuryAmount: 'THIRTY_MILLION' }))}
                    >
                      3,000万円
                    </div>
                    <div 
                      className={`choice-card ${quoteData.personalInjuryAmount === 'FIFTY_MILLION' ? 'selected' : ''} ${errors.personalInjuryAmount ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, personalInjuryAmount: 'FIFTY_MILLION' })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.personalInjuryAmount === 'FIFTY_MILLION'}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, personalInjuryAmount: 'FIFTY_MILLION' }))}
                    >
                      5,000万円（+3,000円）
                    </div>
                    <div 
                      className={`choice-card ${quoteData.personalInjuryAmount === 'UNLIMITED' ? 'selected' : ''} ${errors.personalInjuryAmount ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, personalInjuryAmount: 'UNLIMITED' })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.personalInjuryAmount === 'UNLIMITED'}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, personalInjuryAmount: 'UNLIMITED' }))}
                    >
                      無制限（+7,000円）
                    </div>
                  </div>
                  {errors.personalInjuryAmount && <div className="error-message"><AlertCircle size={14}/>{errors.personalInjuryAmount}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">弁護士費用特約 <span className="required">*</span></label>
                  <div className="choice-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }} role="radiogroup" aria-label="弁護士費用特約">
                    <div 
                      className={`choice-card ${quoteData.lawyerOption === true ? 'selected' : ''} ${errors.lawyerOption ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, lawyerOption: true })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.lawyerOption === true}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, lawyerOption: true }))}
                    >
                      付帯する（+2,000円）
                    </div>
                    <div 
                      className={`choice-card ${quoteData.lawyerOption === false ? 'selected' : ''} ${errors.lawyerOption ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, lawyerOption: false })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.lawyerOption === false}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, lawyerOption: false }))}
                    >
                      付帯しない
                    </div>
                  </div>
                  {errors.lawyerOption && <div className="error-message"><AlertCircle size={14}/>{errors.lawyerOption}</div>}
                </div>

                <div className="form-group">
                  <label className="form-label">ロードサービス <span className="required">*</span></label>
                  <div className="choice-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }} role="radiogroup" aria-label="ロードサービス">
                    <div 
                      className={`choice-card ${quoteData.roadService === true ? 'selected' : ''} ${errors.roadService ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, roadService: true })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.roadService === true}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, roadService: true }))}
                    >
                      付帯する（+1,500円）
                    </div>
                    <div 
                      className={`choice-card ${quoteData.roadService === false ? 'selected' : ''} ${errors.roadService ? 'error-card' : ''}`}
                      onClick={() => setQuoteData({ ...quoteData, roadService: false })}
                      tabIndex={0}
                      role="radio"
                      aria-checked={quoteData.roadService === false}
                      onKeyDown={(e) => handleKeyDown(e, () => setQuoteData({ ...quoteData, roadService: false }))}
                    >
                      付帯しない
                    </div>
                  </div>
                  {errors.roadService && <div className="error-message"><AlertCircle size={14}/>{errors.roadService}</div>}
                </div>

                <div className="btn-group">
                  <button type="button" className="btn btn-secondary" onClick={handleBack}><ArrowLeft size={18}/> 戻る</button>
                  <button type="button" className="btn btn-primary" onClick={handleNext}>次へ進む <ArrowRight size={18}/></button>
                </div>
              </div>
            )}

            {/* SC-006: 入力確認 */}
            {step === 6 && (
              <div className="card">
                <h1>入力内容の最終確認</h1>
                <p className="subtitle">以下の条件で見積書を作成します。内容にお間違いがないかご確認ください。</p>

                <div className="info-row-grid">
                  <div className="info-row">
                    <span className="info-label">運転者年齢</span>
                    <span className="info-value">{quoteData.driverAge} 歳</span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">免許証の色</span>
                    <span className="info-value">{quoteData.licenseColor}</span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">使用目的</span>
                    <span className="info-value">
                      {quoteData.usageType === 'PRIVATE' ? '日常・レジャー' : quoteData.usageType === 'COMMUTE' ? '通勤・通学' : '業務使用'}
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">年間走行距離</span>
                    <span className="info-value">
                      {quoteData.annualMileage === 4000 ? '5,000km以下' : quoteData.annualMileage === 8000 ? '5,001〜10,000km' : '10,001km以上'}
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">運転者範囲</span>
                    <span className="info-value">
                      {quoteData.driverRange === 'SELF' ? '本人限定' : quoteData.driverRange === 'COUPLE' ? '本人・配偶者限定' : quoteData.driverRange === 'FAMILY' ? '同居の親族限定' : '限定なし'}
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">他社自動車保険加入状況</span>
                    <span className="info-value">
                      {quoteData.hasCurrentInsurance ? `加入中 (${quoteData.grade}等級, 事故有期間${quoteData.accidentTerm}年)` : '未加入'}
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">お車</span>
                    <span className="info-value">{quoteData.maker} {quoteData.carName} (登録: {quoteData.firstRegistrationYearMonth})</span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">車両タイプ</span>
                    <span className="info-value">
                      {quoteData.vehicleType === 'KEI' ? '軽自動車' : quoteData.vehicleType === 'COMPACT' ? '小型車' : quoteData.vehicleType === 'SEDAN' ? '普通セダン' : quoteData.vehicleType === 'MINIVAN' ? 'ミニバン' : 'SUV'}
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">車両保険</span>
                    <span className="info-value">{quoteData.vehicleInsurance ? 'あり' : 'なし'}</span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">対物賠償限度額</span>
                    <span className="info-value">{quoteData.propertyDamageLimit === 'UNLIMITED' ? '無制限' : '3,000万円'}</span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">人身傷害補償額</span>
                    <span className="info-value">
                      {quoteData.personalInjuryAmount === 'UNLIMITED' ? '無制限' : quoteData.personalInjuryAmount === 'FIFTY_MILLION' ? '5,000万円' : '3,000万円'}
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">特約</span>
                    <span className="info-value">
                      {[
                        quoteData.lawyerOption && '弁護士特約',
                        quoteData.roadService && 'ロードサービス'
                      ].filter(Boolean).join(', ') || 'なし'}
                    </span>
                  </div>
                </div>

                <div className="btn-group">
                  <button type="button" className="btn btn-secondary" onClick={handleBack}><ArrowLeft size={18}/> 修正する</button>
                  <button type="button" className="btn btn-primary" onClick={handleSubmitQuote}>見積書を作成する <CheckCircle size={18}/></button>
                </div>
              </div>
            )}

            {/* SC-007: 見積結果 */}
            {step === 7 && quoteResult && (
              <div className="card">
                <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
                  <CheckCircle size={48} style={{ color: 'var(--success)', marginBottom: '1rem' }} />
                  <h1>見積り結果のご案内</h1>
                  <p className="subtitle">
                    見積番号: <strong style={{ color: 'var(--text-primary)', background: 'rgba(255,255,255,0.08)', padding: '0.2rem 0.6rem', borderRadius: '4px' }}>{quoteResult.quoteNo}</strong>
                  </p>
                </div>

                <div style={{ background: 'rgba(0,0,0,0.2)', padding: '2rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)', marginBottom: '2rem' }}>
                  <div style={{ textAlign: 'center', fontSize: '1rem', color: 'var(--text-secondary)' }}>お見積り金額（年額）</div>
                  <div className="premium-highlight">¥ {quoteResult.annualPremium.toLocaleString()} <span style={{ fontSize: '1rem', color: 'var(--text-secondary)' }}>円 (税込)</span></div>
                  
                  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', borderTop: '1px solid var(--border)', paddingTop: '1.5rem', marginTop: '0.5rem' }}>
                    <div style={{ textAlign: 'center' }}>
                      <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>月々のお支払目安：</span>
                      <strong style={{ fontSize: '1.5rem', color: 'var(--text-primary)' }}>¥ {quoteResult.monthlyPremium.toLocaleString()}</strong>
                      <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}> 円 / 月</span>
                    </div>
                  </div>
                </div>

                <h2>見積計算の内訳 (Breakdowns)</h2>
                <div className="table-container" style={{ margin: '1rem 0 2rem' }}>
                  <table>
                    <thead>
                      <tr>
                        <th>適用項目</th>
                        <th style={{ textAlign: 'right' }}>係数 / 加算額</th>
                      </tr>
                    </thead>
                    <tbody>
                      {quoteResult.breakdowns.map((b, idx) => (
                        <tr key={idx}>
                          <td>{b.itemName}</td>
                          <td style={{ textAlign: 'right', fontWeight: 600 }}>
                            {b.amount && b.amount > 0 
                              ? `+ ${b.amount.toLocaleString()} 円` 
                              : `× ${b.rate?.toFixed(2)}`}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <div className="btn-group" style={{ justifyContent: 'center' }}>
                  <button type="button" className="btn btn-primary" onClick={handleLogoClick}>新しい見積りを作成する</button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ==================== 2. 管理者ログイン画面 (SC-008) ==================== */}
        {view === 'ADMIN_LOGIN' && (
          <div className="card fade-in" style={{ maxWidth: '450px', margin: '4rem auto' }}>
            <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
              <Lock size={36} style={{ color: 'var(--primary)', marginBottom: '0.5rem' }} />
              <h1>管理者ログイン</h1>
              <p className="subtitle">管理者機能を利用するための資格情報を入力してください。</p>
            </div>

            <form onSubmit={handleAdminLogin}>
              <div className="form-group">
                <label className="form-label">管理者ユーザーID</label>
                <input 
                  type="text" 
                  className="form-control" 
                  required
                  value={loginForm.username}
                  onChange={(e) => setLoginForm({ ...loginForm, username: e.target.value })}
                />
              </div>

              <div className="form-group" style={{ marginBottom: '2rem' }}>
                <label className="form-label">パスワード</label>
                <input 
                  type="password" 
                  className="form-control" 
                  required
                  value={loginForm.password}
                  onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                />
              </div>

              {loginError && <div className="error-message" style={{ marginBottom: '1.5rem' }}><AlertCircle size={16}/>{loginError}</div>}

              <div style={{ display: 'flex', gap: '1rem' }}>
                <button type="button" className="btn btn-secondary" style={{ flex: 1 }} onClick={handleLogoClick}>戻る</button>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>ログイン</button>
              </div>
            </form>
          </div>
        )}

        {/* ==================== 3. 管理者ダッシュボード (SC-009) ==================== */}
        {view === 'ADMIN_DASHBOARD' && (
          <div className="fade-in" style={{ maxWidth: '1200px', width: '100%' }}>
            
            {/* 検索カード */}
            <div className="card" style={{ padding: '2rem', marginBottom: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h1 style={{ margin: 0, fontSize: '1.75rem' }}>見積履歴管理</h1>
                <div style={{ display: 'flex', gap: '0.75rem' }}>
                  <button type="button" className="btn btn-primary" style={{ padding: '0.6rem 1.2rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }} onClick={handleDownloadCsv}>
                    <Download size={16} /> CSV出力
                  </button>
                </div>
              </div>

              {/* 検索フィルタ */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
                <div>
                  <label className="form-label" style={{ fontSize: '0.85rem' }}>見積番号</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    placeholder="例: EST2026..." 
                    value={searchParams.quoteNo}
                    onChange={(e) => setSearchParams({ ...searchParams, quoteNo: e.target.value })}
                  />
                </div>
                <div>
                  <label className="form-label" style={{ fontSize: '0.85rem' }}>メーカー</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    value={searchParams.maker}
                    onChange={(e) => setSearchParams({ ...searchParams, maker: e.target.value })}
                  />
                </div>
                <div>
                  <label className="form-label" style={{ fontSize: '0.85rem' }}>車名</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    value={searchParams.carName}
                    onChange={(e) => setSearchParams({ ...searchParams, carName: e.target.value })}
                  />
                </div>
                <div>
                  <label className="form-label" style={{ fontSize: '0.85rem' }}>運転者年齢</label>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <input 
                      type="number" 
                      className="form-control" 
                      style={{ padding: '0.85rem 0.5rem' }}
                      placeholder="最小"
                      value={searchParams.minAge}
                      onChange={(e) => setSearchParams({ ...searchParams, minAge: e.target.value })}
                    />
                    <span>〜</span>
                    <input 
                      type="number" 
                      className="form-control" 
                      style={{ padding: '0.85rem 0.5rem' }}
                      placeholder="最大" 
                      value={searchParams.maxAge}
                      onChange={(e) => setSearchParams({ ...searchParams, maxAge: e.target.value })}
                    />
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                <button type="button" className="btn btn-secondary" style={{ padding: '0.6rem 1.5rem' }} onClick={() => {
                  setSearchParams({ quoteNo: '', maker: '', carName: '', minAge: '', maxAge: '' });
                }}>クリア</button>
                <button type="button" className="btn btn-primary" style={{ padding: '0.6rem 2rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }} onClick={() => fetchQuotesList()}>
                  <Search size={16} /> 検索
                </button>
              </div>
            </div>

            {/* 結果テーブル */}
            <div className="card" style={{ padding: '1.5rem' }}>
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>見積番号</th>
                      <th>作成日時</th>
                      <th>メーカー/車名</th>
                      <th>年齢</th>
                      <th>免許色</th>
                      <th>年間保険料</th>
                      <th>月額目安</th>
                      <th style={{ textAlign: 'center' }}>詳細</th>
                    </tr>
                  </thead>
                  <tbody>
                    {loading && quotesList.length === 0 ? (
                      <tr>
                        <td colSpan={8}>
                          <InlineLoading rows={3} />
                        </td>
                      </tr>
                    ) : quotesList.length === 0 ? (
                      <tr>
                        <td colSpan={8} style={{ padding: 0 }}>
                          <EmptyState 
                            title="見積データがありません" 
                            description="条件に該当する見積履歴が見つかりませんでした。絞り込み条件をご確認ください。" 
                            actionLabel="検索条件をクリア"
                            onAction={() => setSearchParams({ quoteNo: '', maker: '', carName: '', minAge: '', maxAge: '' })}
                          />
                        </td>
                      </tr>
                    ) : (
                      quotesList.map((q) => (
                        <tr key={q.quoteNo}>
                          <td style={{ fontWeight: 600 }}>{q.quoteNo}</td>
                          <td>{new Date(q.createdAt).toLocaleString('ja-JP', { dateStyle: 'medium', timeStyle: 'short' })}</td>
                          <td>{q.maker || '-'} / {q.carName || '-'}</td>
                          <td>{q.driverAge}歳</td>
                          <td>
                            <span style={{
                              padding: '0.2rem 0.5rem', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 600,
                              background: q.licenseColor === 'GOLD' ? 'rgba(234,179,8,0.2)' : q.licenseColor === 'BLUE' ? 'rgba(59,130,246,0.2)' : 'rgba(16,185,129,0.2)',
                              color: q.licenseColor === 'GOLD' ? '#f59e0b' : q.licenseColor === 'BLUE' ? '#3b82f6' : '#10b981'
                            }}>
                              {q.licenseColor}
                            </span>
                          </td>
                          <td style={{ fontWeight: 600, color: 'var(--secondary)' }}>¥ {q.annualPremium.toLocaleString()}</td>
                          <td>¥ {q.monthlyPremium.toLocaleString()}</td>
                          <td style={{ textAlign: 'center' }}>
                            <button 
                              type="button"
                              className="btn btn-secondary" 
                              style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
                              onClick={() => setSelectedQuoteDetail(q)}
                            >
                              開く
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* 見積詳細ポップアップ */}
            <QuoteDetailModal 
              quote={selectedQuoteDetail} 
              onClose={() => setSelectedQuoteDetail(null)} 
            />

          </div>
        )}
      </main>

      {/* 共通フッター */}
      <footer className="footer">
        <p>&copy; 2026 株式会社ティーアンドエス. All rights reserved.</p>
        <p style={{ marginTop: '0.5rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          ※本アプリは技術検証用であり、実際の保険契約や実料率計算、決済機能は含んでおりません。
        </p>
      </footer>
    </div>
  );
}

/**
 * 見積パラメータ用データ型定義
 */
export interface QuoteData {
  driverAge: number;
  licenseColor: 'GOLD' | 'BLUE' | 'GREEN' | '';
  usageType: 'PRIVATE' | 'COMMUTE' | 'BUSINESS' | '';
  annualMileage: number;
  driverRange: 'SELF' | 'COUPLE' | 'FAMILY' | 'ANYONE' | '';
  hasCurrentInsurance: boolean;
  grade: number | '';
  accidentTerm: number | '';
  maker: string;
  carName: string;
  firstRegistrationYearMonth: string; // YYYY-MM形式
  vehicleType: 'COMPACT' | 'SEDAN' | 'MINIVAN' | 'SUV' | 'KEI' | '';
  vehicleInsurance: boolean;
  propertyDamageLimit: 'UNLIMITED' | 'THIRTY_MILLION' | '';
  personalInjuryAmount: 'THIRTY_MILLION' | 'FIFTY_MILLION' | 'UNLIMITED' | '';
  lawyerOption: boolean;
  roadService: boolean;
}

/**
 * 見積計算応答の内訳データ型定義
 */
export interface BreakdownDto {
  itemCode: string;
  itemName: string;
  rate: number | null;
  amount: number | null;
}

/**
 * 見積結果レスポンスデータ型定義
 */
export interface QuoteResponseData {
  quoteNo: string;
  driverAge: number;
  licenseColor: string;
  usageType: string;
  annualMileage: number;
  driverRange: string;
  hasCurrentInsurance: boolean;
  grade: number | null;
  accidentTerm: number | null;
  maker: string;
  carName: string;
  firstRegistrationYearMonth: string;
  vehicleType: string;
  vehicleInsurance: boolean;
  annualPremium: number;
  monthlyPremium: number;
  breakdowns: BreakdownDto[];
  createdAt: string;
}

/**
 * 管理者ログイン用レスポンスデータ型定義
 */
export interface LoginResponseData {
  token: string;
  username: string;
}

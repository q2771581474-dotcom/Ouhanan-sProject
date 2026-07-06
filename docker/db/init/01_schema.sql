-- =================================================================
-- 自動車保険見積システム データベース定義 (DDL)
-- PostgreSQL 16 対応
-- 【設計方針】CREATE TABLE IF NOT EXISTS により、コンテナ再起動時や
-- 既にボリュームが存在する場合でもエラーを発生させずにスキップする冪等設計。
-- =================================================================

-- -----------------------------------------------------------------
-- 1. 管理者ユーザーテーブル (admin_users)
-- 【セキュリティ設計】パスワードカラムは平文保存を防ぐため、
-- 十分な長さ(VARCHAR(100))を確保し、アプリケーション側でBCryptハッシュ化して格納する。
-- -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------
-- 2. 料率マスタテーブル (rate_masters)
-- 【パフォーマンス設計】見積計算時に高頻度で参照されるため、
-- 有効なマスタレコード（active=TRUE）のみを対象とした部分インデックス(Partial Index)を
-- 定義し、インデックスサイズを最小化しつつ検索を高速化する。
-- 【精度設計】小数点以下の乗算倍率(例: 1.600)の演算精度を保証し、
-- 浮点数による丸め誤差を防ぐために、NUMERIC(6,3)型を採用する。
-- -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rate_masters (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,    -- 区分: AGE / LICENSE / USAGE / MILEAGE / RANGE / GRADE / ACCIDENT / VEHICLE_TYPE / COVERAGE
    item_code VARCHAR(50) NOT NULL,   -- キー: 18_25 / GOLD / PRIVATE / COMPACT 等
    item_name VARCHAR(100) NOT NULL,  -- 表示名: 18〜25歳 / ゴールド 等
    rate NUMERIC(6,3),                -- 乗算係数 (例: 1.600)
    amount INTEGER,                   -- 加算定額 (例: 30000)
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_rate_masters_lookup ON rate_masters(category, item_code) WHERE active = TRUE;

-- -----------------------------------------------------------------
-- 3. 見積ヘッダテーブル (quotes)
-- 【設計意図】一般利用者が作成した見積の一時保存および管理者履歴検索の親テーブル。
-- 初度登録年月(first_registration_ym)は「YYYY-MM」形式で固定されるため、
-- 可変長ではなくCHAR(7)を採用し、ストレージ効率と整合性を担保する。
-- -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS quotes (
    id BIGSERIAL PRIMARY KEY,
    quote_no VARCHAR(20) UNIQUE NOT NULL,   -- 採番形式: ESTyyyyMMdd0001
    driver_age INTEGER NOT NULL,
    license_color VARCHAR(20) NOT NULL,
    usage_type VARCHAR(20) NOT NULL,
    annual_mileage INTEGER NOT NULL,
    driver_range VARCHAR(20) NOT NULL,
    has_current_insurance BOOLEAN NOT NULL,
    grade INTEGER,                           -- 現在加入なしの場合はNULL
    accident_term INTEGER,                   -- 事故有係数期間（年）、該当なしはNULL
    maker VARCHAR(50) NOT NULL,
    car_name VARCHAR(50) NOT NULL,
    first_registration_ym CHAR(7) NOT NULL, -- 形式: YYYY-MM
    vehicle_type VARCHAR(20) NOT NULL,
    vehicle_insurance BOOLEAN NOT NULL,
    annual_premium INTEGER NOT NULL,
    monthly_premium INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 高頻度で検索・ソートされる見積番号および作成日時にインデックスを設定
CREATE INDEX IF NOT EXISTS idx_quotes_no ON quotes(quote_no);
CREATE INDEX IF NOT EXISTS idx_quotes_created_at ON quotes(created_at);

-- -----------------------------------------------------------------
-- 4. 見積計算内訳テーブル (quote_breakdowns)
-- 【データ完全性設計】見積ヘッダテーブル(quotes)への外部キー参照を設定。
-- quotesの親レコードが削除された場合、関連する明细レコードも自動的に連動削除
-- （ON DELETE CASCADE）されるよう定義し、孤立レコード(Orphan Records)の発生を防止する。
-- -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS quote_breakdowns (
    id BIGSERIAL PRIMARY KEY,
    quote_id BIGINT NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    rate NUMERIC(6,3),
    amount INTEGER,
    display_order INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_quote_breakdowns_quote ON quote_breakdowns(quote_id);

-- =====================================================
-- TaSProject データベーススキーマ (DDL)
-- PostgreSQL 16 対応
-- 【設計方針】CREATE TABLE IF NOT EXISTS により、
-- 重複実行時でも安全にスキップされる冪等設計。
-- =====================================================

-- 1. 管理者ユーザーテーブル
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL, -- BCryptハッシュ値を格納
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. 料率マスタテーブル
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

-- 3. 見積ヘッダテーブル
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

CREATE INDEX IF NOT EXISTS idx_quotes_no ON quotes(quote_no);
CREATE INDEX IF NOT EXISTS idx_quotes_created_at ON quotes(created_at);

-- 4. 見積計算内訳テーブル
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

-- 1. 管理者ユーザーの初期データ
-- ユーザー名: admin, パスワード: admin123 (Spring Boot起動時にDatabaseInitializerが自動生成・暗号化して登録します)


-- 2. 料率マスタの初期データ
-- 年齢区分 (AGE)
INSERT INTO rate_masters (category, item_code, item_name, rate, amount) VALUES
('AGE', '18_25', '18〜25歳', 1.60, 0),
('AGE', '26_34', '26〜34歳', 1.25, 0),
('AGE', '35_59', '35〜59歳', 1.00, 0),
('AGE', '60_UP', '60歳以上', 1.20, 0);

-- 免許証色 (LICENSE)
INSERT INTO rate_masters (category, item_code, item_name, rate, amount) VALUES
('LICENSE', 'GOLD', 'ゴールド', 0.90, 0),
('LICENSE', 'BLUE', 'ブルー', 1.00, 0),
('LICENSE', 'GREEN', 'グリーン', 1.10, 0);

-- 使用目的 (USAGE)
INSERT INTO rate_masters (category, item_code, item_name, rate, amount) VALUES
('USAGE', 'PRIVATE', '日常・レジャー用', 1.00, 0),
('USAGE', 'COMMUTE', '通勤・通学用', 1.10, 0),
('USAGE', 'BUSINESS', '業務用', 1.25, 0);

-- 年間走行距離 (MILEAGE)
INSERT INTO rate_masters (category, item_code, item_name, rate, amount) VALUES
('MILEAGE', '0_5000', '5,000km以下', 0.95, 0),
('MILEAGE', '5001_10000', '5,001km〜10,000km', 1.00, 0),
('MILEAGE', '10001_UP', '10,001km以上', 1.15, 0);

-- 運転者範囲 (RANGE)
INSERT INTO rate_masters (category, item_code, item_name, rate, amount) VALUES
('RANGE', 'SELF', '本人限定', 0.90, 0),
('RANGE', 'COUPLE', '本人・配偶者限定', 0.95, 0),
('RANGE', 'FAMILY', '同居の親族限定', 1.05, 0),
('RANGE', 'ANYONE', '限定なし', 1.20, 0);

-- 等級 (GRADE)
INSERT INTO rate_masters (category, item_code, item_name, rate, amount) VALUES
('GRADE', '1_5', '1〜5等級', 1.30, 0),
('GRADE', '6_10', '6〜10等級', 1.10, 0),
('GRADE', '11_15', '11〜15等級', 0.95, 0),
('GRADE', '16_20', '16〜20等級', 0.80, 0);

-- 事故有係数期間 (ACCIDENT)
INSERT INTO rate_masters (category, item_code, item_name, rate, amount) VALUES
('ACCIDENT', 'TERM_1_UP', '事故有期間1年以上', 1.20, 0);

-- 車両タイプ (VEHICLE_TYPE)
INSERT INTO rate_masters (category, item_code, item_name, rate, amount) VALUES
('VEHICLE_TYPE', 'KEI', '軽自動車', 0.90, 0),
('VEHICLE_TYPE', 'COMPACT', '小型乗用車', 0.95, 0),
('VEHICLE_TYPE', 'SEDAN', '普通乗用車（セダン）', 1.00, 0),
('VEHICLE_TYPE', 'MINIVAN', 'ミニバン', 1.10, 0),
('VEHICLE_TYPE', 'SUV', 'SUV', 1.15, 0);

-- 補償・特約加算額 (COVERAGE)
INSERT INTO rate_masters (category, item_code, item_name, rate, amount) VALUES
('COVERAGE', 'VEHICLE_INSURANCE_TRUE', '車両保険あり', 1.00, 30000),
('COVERAGE', 'PROPERTY_DAMAGE_UNLIMITED', '対物無制限加算', 1.00, 5000),
('COVERAGE', 'PERSONAL_INJURY_50M', '人身傷害5000万加算', 1.00, 3000),
('COVERAGE', 'PERSONAL_INJURY_UNLIMITED', '人身傷害無制限加算', 1.00, 7000),
('COVERAGE', 'LAWYER_OPTION_TRUE', '弁護士特約あり', 1.00, 2000),
('COVERAGE', 'ROAD_SERVICE_TRUE', 'ロードサービスあり', 1.00, 1500);

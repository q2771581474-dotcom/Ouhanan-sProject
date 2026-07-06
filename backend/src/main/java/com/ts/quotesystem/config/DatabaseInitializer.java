package com.ts.quotesystem.config;

import com.ts.quotesystem.entity.AdminUser;
import com.ts.quotesystem.entity.RateMaster;
import com.ts.quotesystem.repository.AdminUserRepository;
import com.ts.quotesystem.repository.RateMasterRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * データベースの初期データを登録する初期化クラス
 * 起動時にデータが存在しない場合に自動実行される
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final RateMasterRepository rateMasterRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(AdminUserRepository adminUserRepository,
                               RateMasterRepository rateMasterRepository,
                               PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.rateMasterRepository = rateMasterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
        initializeRateMasters();
    }

    /**
     * 【セキュリティ・運用設計】デプロイ直後でも認証機能が即座に動作するよう、初期管理アカウントをBCryptハッシュ化して安全にシード生成する。
     */
    private void initializeAdminUser() {
        if (adminUserRepository.count() == 0) {
            AdminUser admin = AdminUser.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .build();
            adminUserRepository.save(admin);
            System.out.println("Default admin user created successfully.");
        }
    }

    /**
     * 【設計考量】仕様書(dos/中文版.md)に定義されたすべての保険料率及び加算定額マスタを初期化し、見積エンジンの稼働を保証する。
     */
    private void initializeRateMasters() {
        if (rateMasterRepository.count() == 0) {
            List<RateMaster> rates = new ArrayList<>();

            // 年齢区分 (AGE)
            rates.add(createRateMaster("AGE", "18_25", "18〜25歳", 1.60, 0));
            rates.add(createRateMaster("AGE", "26_34", "26〜34歳", 1.25, 0));
            rates.add(createRateMaster("AGE", "35_59", "35〜59歳", 1.00, 0));
            rates.add(createRateMaster("AGE", "60_UP", "60歳以上", 1.20, 0));

            // 免許証色 (LICENSE)
            rates.add(createRateMaster("LICENSE", "GOLD", "ゴールド", 0.90, 0));
            rates.add(createRateMaster("LICENSE", "BLUE", "ブルー", 1.00, 0));
            rates.add(createRateMaster("LICENSE", "GREEN", "グリーン", 1.10, 0));

            // 使用目的 (USAGE)
            rates.add(createRateMaster("USAGE", "PRIVATE", "日常・レジャー用", 1.00, 0));
            rates.add(createRateMaster("USAGE", "COMMUTE", "通勤・通学用", 1.10, 0));
            rates.add(createRateMaster("USAGE", "BUSINESS", "業務用", 1.25, 0));

            // 年間走行距離 (MILEAGE)
            rates.add(createRateMaster("MILEAGE", "0_5000", "5,000km以下", 0.95, 0));
            rates.add(createRateMaster("MILEAGE", "5001_10000", "5,001km〜10,000km", 1.00, 0));
            rates.add(createRateMaster("MILEAGE", "10001_UP", "10,001km以上", 1.15, 0));

            // 運転者範囲 (RANGE)
            rates.add(createRateMaster("RANGE", "SELF", "本人限定", 0.90, 0));
            rates.add(createRateMaster("RANGE", "COUPLE", "本人・配偶者限定", 0.95, 0));
            rates.add(createRateMaster("RANGE", "FAMILY", "同居の親族限定", 1.05, 0));
            rates.add(createRateMaster("RANGE", "ANYONE", "限定なし", 1.20, 0));

            // 等級 (GRADE)
            rates.add(createRateMaster("GRADE", "1_5", "1〜5等級", 1.30, 0));
            rates.add(createRateMaster("GRADE", "6_10", "6〜10等級", 1.10, 0));
            rates.add(createRateMaster("GRADE", "11_15", "11〜15等級", 0.95, 0));
            rates.add(createRateMaster("GRADE", "16_20", "16〜20等級", 0.80, 0));

            // 事故有係数期間 (ACCIDENT)
            rates.add(createRateMaster("ACCIDENT", "TERM_1_UP", "事故有期間1年以上", 1.20, 0));

            // 車両タイプ (VEHICLE_TYPE)
            rates.add(createRateMaster("VEHICLE_TYPE", "KEI", "軽自動車", 0.90, 0));
            rates.add(createRateMaster("VEHICLE_TYPE", "COMPACT", "小型乗用車", 0.95, 0));
            rates.add(createRateMaster("VEHICLE_TYPE", "SEDAN", "普通乗用車（セダン）", 1.00, 0));
            rates.add(createRateMaster("VEHICLE_TYPE", "MINIVAN", "ミニバン", 1.10, 0));
            rates.add(createRateMaster("VEHICLE_TYPE", "SUV", "SUV", 1.15, 0));

            // 補償・特約加算額 (COVERAGE)
            rates.add(createRateMaster("COVERAGE", "VEHICLE_INSURANCE_TRUE", "車両保険あり", 1.00, 30000));
            rates.add(createRateMaster("COVERAGE", "PROPERTY_DAMAGE_UNLIMITED", "対物無制限加算", 1.00, 5000));
            rates.add(createRateMaster("COVERAGE", "PERSONAL_INJURY_50M", "人身傷害5000万加算", 1.00, 3000));
            rates.add(createRateMaster("COVERAGE", "PERSONAL_INJURY_UNLIMITED", "人身傷害無制限加算", 1.00, 7000));
            rates.add(createRateMaster("COVERAGE", "LAWYER_OPTION_TRUE", "弁護士特約あり", 1.00, 2000));
            rates.add(createRateMaster("COVERAGE", "ROAD_SERVICE_TRUE", "ロードサービスあり", 1.00, 1500));

            rateMasterRepository.saveAll(rates);
            System.out.println("Default rate masters initialized successfully.");
        }
    }

    private RateMaster createRateMaster(String category, String itemCode, String itemName, double rate, int amount) {
        return RateMaster.builder()
                .category(category)
                .itemCode(itemCode)
                .itemName(itemName)
                .rate(BigDecimal.valueOf(rate))
                .amount(amount)
                .active(true)
                .build();
    }
}

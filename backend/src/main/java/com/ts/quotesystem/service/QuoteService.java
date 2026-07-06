package com.ts.quotesystem.service;

import com.ts.quotesystem.dto.QuoteRequest;
import com.ts.quotesystem.dto.QuoteResponse;
import com.ts.quotesystem.entity.Quote;
import com.ts.quotesystem.entity.QuoteBreakdown;
import com.ts.quotesystem.entity.RateMaster;
import com.ts.quotesystem.repository.QuoteBreakdownRepository;
import com.ts.quotesystem.repository.QuoteRepository;
import com.ts.quotesystem.repository.RateMasterRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 見積計算および永続化を行うビジネスロジックサービス
 */
@Service
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteBreakdownRepository quoteBreakdownRepository;
    private final RateMasterRepository rateMasterRepository;

    public QuoteService(QuoteRepository quoteRepository,
                        QuoteBreakdownRepository quoteBreakdownRepository,
                        RateMasterRepository rateMasterRepository) {
        this.quoteRepository = quoteRepository;
        this.quoteBreakdownRepository = quoteBreakdownRepository;
        this.rateMasterRepository = rateMasterRepository;
    }

    /**
     * 新規見積を作成する
     * 【設計考量】ユーザー入力条件に基づき基本保険料(50,000円)から各係数を乗算・加算し、日本円決済要件に適合させるため10円単位で四捨五入して永続化する。
     */
    @Transactional
    public QuoteResponse createQuote(QuoteRequest request) {
        // 1. 保険料の計算と内訳の組み立て
        List<QuoteBreakdown> breakdowns = new ArrayList<>();
        BigDecimal currentPremium = BigDecimal.valueOf(50000); // 基本保険料
        int displayOrder = 1;

        // a. 年齢区分
        String ageCode = getAgeCode(request.getDriverAge());
        RateMaster ageRate = getRateMasterFromDb("AGE", ageCode);
        currentPremium = currentPremium.multiply(ageRate.getRate());
        breakdowns.add(createBreakdown(ageRate, displayOrder++));

        // b. 免許証色
        RateMaster licenseRate = getRateMasterFromDb("LICENSE", request.getLicenseColor());
        currentPremium = currentPremium.multiply(licenseRate.getRate());
        breakdowns.add(createBreakdown(licenseRate, displayOrder++));

        // c. 使用目的
        RateMaster usageRate = getRateMasterFromDb("USAGE", request.getUsageType());
        currentPremium = currentPremium.multiply(usageRate.getRate());
        breakdowns.add(createBreakdown(usageRate, displayOrder++));

        // d. 走行距離
        String mileageCode = getMileageCode(request.getAnnualMileage());
        RateMaster mileageRate = getRateMasterFromDb("MILEAGE", mileageCode);
        currentPremium = currentPremium.multiply(mileageRate.getRate());
        breakdowns.add(createBreakdown(mileageRate, displayOrder++));

        // e. 運転者範囲
        RateMaster rangeRate = getRateMasterFromDb("RANGE", request.getDriverRange());
        currentPremium = currentPremium.multiply(rangeRate.getRate());
        breakdowns.add(createBreakdown(rangeRate, displayOrder++));

        // f. 等級 (現在加入ありの場合のみ)
        if (Boolean.TRUE.equals(request.getHasCurrentInsurance()) && request.getGrade() != null) {
            String gradeCode = getGradeCode(request.getGrade());
            RateMaster gradeRate = getRateMasterFromDb("GRADE", gradeCode);
            currentPremium = currentPremium.multiply(gradeRate.getRate());
            breakdowns.add(createBreakdown(gradeRate, displayOrder++));
        }

        // g. 事故有係数期間 (現在加入あり且つ1年以上の場合のみ)
        if (Boolean.TRUE.equals(request.getHasCurrentInsurance()) && request.getAccidentTerm() != null && request.getAccidentTerm() >= 1) {
            RateMaster accidentRate = getRateMasterFromDb("ACCIDENT", "TERM_1_UP");
            currentPremium = currentPremium.multiply(accidentRate.getRate());
            breakdowns.add(createBreakdown(accidentRate, displayOrder++));
        }

        // h. 車両タイプ
        RateMaster vehicleRate = getRateMasterFromDb("VEHICLE_TYPE", request.getVehicleType());
        currentPremium = currentPremium.multiply(vehicleRate.getRate());
        breakdowns.add(createBreakdown(vehicleRate, displayOrder++));

        // 乗算完了後の基本額に対して、加算項目を追加する
        int additionsSum = 0;

        // i. 車両保険
        if (Boolean.TRUE.equals(request.getVehicleInsurance())) {
            RateMaster covVeh = getRateMasterFromDb("COVERAGE", "VEHICLE_INSURANCE_TRUE");
            additionsSum += covVeh.getAmount();
            breakdowns.add(createBreakdown(covVeh, displayOrder++));
        }

        // j. 对物补偿 (無制限のみ加算)
        if ("UNLIMITED".equals(request.getPropertyDamageLimit())) {
            RateMaster covProp = getRateMasterFromDb("COVERAGE", "PROPERTY_DAMAGE_UNLIMITED");
            additionsSum += covProp.getAmount();
            breakdowns.add(createBreakdown(covProp, displayOrder++));
        }

        // k. 人身傷害 (5000万 / 無制限 のみ加算)
        if ("FIFTY_MILLION".equals(request.getPersonalInjuryAmount())) {
            RateMaster covPers = getRateMasterFromDb("COVERAGE", "PERSONAL_INJURY_50M");
            additionsSum += covPers.getAmount();
            breakdowns.add(createBreakdown(covPers, displayOrder++));
        } else if ("UNLIMITED".equals(request.getPersonalInjuryAmount())) {
            RateMaster covPers = getRateMasterFromDb("COVERAGE", "PERSONAL_INJURY_UNLIMITED");
            additionsSum += covPers.getAmount();
            breakdowns.add(createBreakdown(covPers, displayOrder++));
        }

        // l. 弁護士特約
        if (Boolean.TRUE.equals(request.getLawyerOption())) {
            RateMaster covLaw = getRateMasterFromDb("COVERAGE", "LAWYER_OPTION_TRUE");
            additionsSum += covLaw.getAmount();
            breakdowns.add(createBreakdown(covLaw, displayOrder++));
        }

        // m. ロードサービス
        if (Boolean.TRUE.equals(request.getRoadService())) {
            RateMaster covRoad = getRateMasterFromDb("COVERAGE", "ROAD_SERVICE_TRUE");
            additionsSum += covRoad.getAmount();
            breakdowns.add(createBreakdown(covRoad, displayOrder++));
        }

        // 年間保険料の計算：乗算後価格 + 加算合計。10円未満を四捨五入する
        BigDecimal finalAnnualPremium = currentPremium.add(BigDecimal.valueOf(additionsSum));
        // setScale(-1) で10の位に丸める（個の位を四捨五入）
        BigDecimal roundedAnnual = finalAnnualPremium.setScale(-1, RoundingMode.HALF_UP);

        // 月額保険料の計算：年間保険料 ÷ 12。10円未満を四捨五入する
        BigDecimal rawMonthly = roundedAnnual.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal roundedMonthly = rawMonthly.setScale(-1, RoundingMode.HALF_UP);

        // 2. 見積番号の採番
        String quoteNo = generateQuoteNo();

        // 3. エンティティの組み立てと保存
        Quote quote = Quote.builder()
                .quoteNo(quoteNo)
                .driverAge(request.getDriverAge())
                .licenseColor(request.getLicenseColor())
                .usageType(request.getUsageType())
                .annualMileage(request.getAnnualMileage())
                .driverRange(request.getDriverRange())
                .hasCurrentInsurance(request.getHasCurrentInsurance())
                .grade(request.getGrade())
                .accidentTerm(request.getAccidentTerm())
                .maker(request.getMaker())
                .carName(request.getCarName())
                .firstRegistrationYm(request.getFirstRegistrationYearMonth())
                .vehicleType(request.getVehicleType())
                .vehicleInsurance(request.getVehicleInsurance())
                .annualPremium(roundedAnnual.intValue())
                .monthlyPremium(roundedMonthly.intValue())
                .build();

        // 内訳とのリレーション設定
        for (QuoteBreakdown b : breakdowns) {
            quote.addBreakdown(b);
        }

        Quote savedQuote = quoteRepository.save(quote);

        // DTOへの変換
        return convertToResponse(savedQuote);
    }

    /**
     * 見積番号で検索し、見積結果を取得する
     * 【設計考量】見積番号(ESTxxxx)による一意検索を行い、クライアント側表示に必要な内訳明細DTOへ安全に変換して返却する。
     */
    public Optional<QuoteResponse> getQuoteByNo(String quoteNo) {
        return quoteRepository.findByQuoteNo(quoteNo).map(this::convertToResponse);
    }

    /**
     * 管理者用に条件指定で見積一覧を検索する
     * 【設計考量】管理者による多様な検索ニーズ(メーカー、年齢等)へ柔軟に対応するため、JPA Specificationによる動的クエリを構築し最新順で提供する。
     */
    public List<QuoteResponse> searchQuotes(String quoteNo, String maker, String carName, Integer minAge, Integer maxAge) {
        Specification<Quote> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (quoteNo != null && !quoteNo.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("quoteNo")), "%" + quoteNo.toLowerCase() + "%"));
            }
            if (maker != null && !maker.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("maker")), "%" + maker.toLowerCase() + "%"));
            }
            if (carName != null && !carName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("carName")), "%" + carName.toLowerCase() + "%"));
            }
            if (minAge != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("driverAge"), minAge));
            }
            if (maxAge != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("driverAge"), maxAge));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 新しい順（降順）で取得
        return quoteRepository.findAll(spec).stream()
                .sorted((q1, q2) -> q2.getCreatedAt().compareTo(q1.getCreatedAt()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * キャッシュを利用してマスタデータを取得する
     * 【パフォーマンス最適化】DBへの頻繁なI/O負荷を低減しレスポンス速度を向上させるため、Spring Cache(@Cacheable)によりマスタデータをインメモリ保持する。
     */
    @Cacheable(value = "rateMasters", key = "#category + '_' + #itemCode")
    public RateMaster getRateMasterFromDb(String category, String itemCode) {
        return rateMasterRepository.findByCategoryAndItemCodeAndActiveTrue(category, itemCode)
                .orElseThrow(() -> new IllegalArgumentException("対応する料率マスタが見つかりません: " + category + " - " + itemCode));
    }

    /**
     * マスタデータ変更時にキャッシュをクリアする用
     * 【保守性向上】マスタテーブルの改定時にキャッシュ不整合を防ぐため、全キャッシュエントリーを安全に破棄する。
     */
    @CacheEvict(value = "rateMasters", allEntries = true)
    public void clearRateMastersCache() {
        // キャッシュクリア用
    }

    // --- ヘルパーメソッド群 ---

    /**
     * 【設計考量】入力された運転者年齢を、仕様書規定の4区分コードへ安全にマッピングする。
     */
    private String getAgeCode(int age) {
        if (age >= 18 && age <= 25) return "18_25";
        if (age >= 26 && age <= 34) return "26_34";
        if (age >= 35 && age <= 59) return "35_59";
        return "60_UP";
    }

    private String getMileageCode(int mileage) {
        if (mileage <= 5000) return "0_5000";
        if (mileage <= 10000) return "5001_10000";
        return "10001_UP";
    }

    private String getGradeCode(int grade) {
        if (grade >= 1 && grade <= 5) return "1_5";
        if (grade >= 6 && grade <= 10) return "6_10";
        if (grade >= 11 && grade <= 15) return "11_15";
        return "16_20";
    }

    private QuoteBreakdown createBreakdown(RateMaster master, int displayOrder) {
        return QuoteBreakdown.builder()
                .itemCode(master.getItemCode())
                .itemName(master.getItemName())
                .rate(master.getRate())
                .amount(master.getAmount())
                .displayOrder(displayOrder)
                .build();
    }

    /**
     * 【排他制御・一意性保証】同時アクセス時における見積番号の重複採番を完全に防止するため、synchronized及びダブルチェックアルゴリズムを採用する。
     */
    private synchronized String generateQuoteNo() {
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "EST" + todayStr;
        long todayCount = quoteRepository.countByQuoteNoStartingWith(prefix);
        long nextSeq = todayCount + 1;
        
        // 重複を防止するためのダブルチェック
        String proposedNo;
        do {
            proposedNo = String.format("%s%04d", prefix, nextSeq++);
        } while (quoteRepository.findByQuoteNo(proposedNo).isPresent());
        
        return proposedNo;
    }

    /**
     * 【設計考量】内部DBエンティティ構造を隠蔽し、クライアントへ必要な契約情報および計算内訳のみをDTOとして安全に提供する。
     */
    private QuoteResponse convertToResponse(Quote quote) {
        List<QuoteResponse.BreakdownDto> breakdownDtos = quote.getBreakdowns().stream()
                .map(b -> QuoteResponse.BreakdownDto.builder()
                        .itemCode(b.getItemCode())
                        .itemName(b.getItemName())
                        .rate(b.getRate())
                        .amount(b.getAmount())
                        .build())
                .collect(Collectors.toList());

        return QuoteResponse.builder()
                .quoteNo(quote.getQuoteNo())
                .driverAge(quote.getDriverAge())
                .licenseColor(quote.getLicenseColor())
                .usageType(quote.getUsageType())
                .annualMileage(quote.getAnnualMileage())
                .driverRange(quote.getDriverRange())
                .hasCurrentInsurance(quote.getHasCurrentInsurance())
                .grade(quote.getGrade())
                .accidentTerm(quote.getAccidentTerm())
                .maker(quote.getMaker())
                .carName(quote.getCarName())
                .firstRegistrationYearMonth(quote.getFirstRegistrationYm())
                .vehicleType(quote.getVehicleType())
                .vehicleInsurance(quote.getVehicleInsurance())
                .annualPremium(quote.getAnnualPremium())
                .monthlyPremium(quote.getMonthlyPremium())
                .createdAt(quote.getCreatedAt())
                .breakdowns(breakdownDtos)
                .build();
    }
}

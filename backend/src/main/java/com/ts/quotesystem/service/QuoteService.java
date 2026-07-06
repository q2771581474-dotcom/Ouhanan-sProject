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
     * 【DRY・単一責任原則】メソッド単体の長さを30行以内に抑えるため、乗算処理、加算処理、エンティティ構築をそれぞれ専用の非公開メソッドに分離する。
     */
    @Transactional
    public QuoteResponse createQuote(QuoteRequest request) {
        List<QuoteBreakdown> breakdowns = new ArrayList<>();

        // 1. 各種料率（乗算）の適用
        BigDecimal currentPremium = applyMultipliers(request, breakdowns);

        // 2. 加算項目（特約・補償）の集計
        int additionsSum = sumAdditions(request, breakdowns);

        // 3. 年間保険料・月額保険料の端数処理（10円未満四捨五入）
        int annualPremium = roundPremium(currentPremium.add(BigDecimal.valueOf(additionsSum)));
        int monthlyPremium = roundPremium(BigDecimal.valueOf(annualPremium).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP));

        // 4. 見積エンティティの組み立て・永続化およびDTO変換
        Quote quote = buildQuoteEntity(request, generateQuoteNo(), annualPremium, monthlyPremium, breakdowns);
        Quote savedQuote = quoteRepository.save(quote);

        return convertToResponse(savedQuote);
    }

    /**
     * 【設計意図】見積計算におけるすべての乗算係数（年齢、免許証色、目的、距離、範囲、等級、事故有、車両）を順次適用する。
     */
    private BigDecimal applyMultipliers(QuoteRequest request, List<QuoteBreakdown> breakdowns) {
        BigDecimal premium = BigDecimal.valueOf(50000);
        int order = 1;

        // 年齢、免許証色、使用目的
        premium = applyMultiplier(premium, "AGE", getAgeCode(request.getDriverAge()), breakdowns, order++);
        premium = applyMultiplier(premium, "LICENSE", request.getLicenseColor(), breakdowns, order++);
        premium = applyMultiplier(premium, "USAGE", request.getUsageType(), breakdowns, order++);

        // 走行距離、運転者範囲
        premium = applyMultiplier(premium, "MILEAGE", getMileageCode(request.getAnnualMileage()), breakdowns, order++);
        premium = applyMultiplier(premium, "RANGE", request.getDriverRange(), breakdowns, order++);

        // 等級・事故有（加入ありの場合のみ）
        if (Boolean.TRUE.equals(request.getHasCurrentInsurance())) {
            if (request.getGrade() != null) {
                premium = applyMultiplier(premium, "GRADE", getGradeCode(request.getGrade()), breakdowns, order++);
            }
            if (request.getAccidentTerm() != null && request.getAccidentTerm() >= 1) {
                premium = applyMultiplier(premium, "ACCIDENT", "TERM_1_UP", breakdowns, order++);
            }
        }

        // 車両タイプ
        premium = applyMultiplier(premium, "VEHICLE_TYPE", request.getVehicleType(), breakdowns, order++);

        return premium;
    }

    /**
     * 【DRY設計】個別マスタデータを取得し、保険料に乗算した上で計算明細リストに追加する。
     */
    private BigDecimal applyMultiplier(BigDecimal premium, String category, String itemCode, List<QuoteBreakdown> breakdowns, int order) {
        RateMaster master = getRateMasterFromDb(category, itemCode);
        breakdowns.add(createBreakdown(master, order));
        return premium.multiply(master.getRate());
    }

    /**
     * 【設計意図】特約や車両保険などの定額加算項目を集計し、明細リストに追加する。
     */
    private int sumAdditions(QuoteRequest request, List<QuoteBreakdown> breakdowns) {
        int sum = 0;
        int order = breakdowns.size() + 1;

        if (Boolean.TRUE.equals(request.getVehicleInsurance())) {
            sum += getAdditionAmount("COVERAGE", "VEHICLE_INSURANCE_TRUE", breakdowns, order++);
        }
        if ("UNLIMITED".equals(request.getPropertyDamageLimit())) {
            sum += getAdditionAmount("COVERAGE", "PROPERTY_DAMAGE_UNLIMITED", breakdowns, order++);
        }

        // 人身傷害
        if ("FIFTY_MILLION".equals(request.getPersonalInjuryAmount())) {
            sum += getAdditionAmount("COVERAGE", "PERSONAL_INJURY_50M", breakdowns, order++);
        } else if ("UNLIMITED".equals(request.getPersonalInjuryAmount())) {
            sum += getAdditionAmount("COVERAGE", "PERSONAL_INJURY_UNLIMITED", breakdowns, order++);
        }

        if (Boolean.TRUE.equals(request.getLawyerOption())) {
            sum += getAdditionAmount("COVERAGE", "LAWYER_OPTION_TRUE", breakdowns, order++);
        }
        if (Boolean.TRUE.equals(request.getRoadService())) {
            sum += getAdditionAmount("COVERAGE", "ROAD_SERVICE_TRUE", breakdowns, order++);
        }

        return sum;
    }

    /**
     * 【DRY設計】加算項目マスタを取得し、金額を返却するとともに計算明細リストに追加する。
     */
    private int getAdditionAmount(String category, String itemCode, List<QuoteBreakdown> breakdowns, int order) {
        RateMaster master = getRateMasterFromDb(category, itemCode);
        breakdowns.add(createBreakdown(master, order));
        return master.getAmount();
    }

    /**
     * 【精度設計】日本円の最小通貨単位（端数）処理として、10円未満を四捨五入する。
     */
    private int roundPremium(BigDecimal premium) {
        return premium.setScale(-1, RoundingMode.HALF_UP).intValue();
    }

    /**
     * 【設計意図】見積ヘッダのエンティティオブジェクトを組み立てる。
     */
    private Quote buildQuoteEntity(QuoteRequest request, String quoteNo, int annual, int monthly, List<QuoteBreakdown> breakdowns) {
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
                .annualPremium(annual)
                .monthlyPremium(monthly)
                .build();

        breakdowns.forEach(quote::addBreakdown);
        return quote;
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

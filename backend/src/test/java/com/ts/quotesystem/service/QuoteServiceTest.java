package com.ts.quotesystem.service;

import com.ts.quotesystem.dto.QuoteRequest;
import com.ts.quotesystem.dto.QuoteResponse;
import com.ts.quotesystem.entity.Quote;
import com.ts.quotesystem.repository.QuoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class QuoteServiceTest {

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private QuoteRepository quoteRepository;

    /**
     * UT-001: 保険料計算（標準的な低リスク条件）
     */
    @Test
    public void testUt001_StandardCalculation() {
        QuoteRequest request = QuoteRequest.builder()
                .driverAge(35)
                .licenseColor("GOLD")
                .usageType("PRIVATE")
                .annualMileage(8000)
                .driverRange("SELF")
                .hasCurrentInsurance(true)
                .grade(20)
                .accidentTerm(0)
                .maker("トヨタ")
                .carName("プリウス")
                .firstRegistrationYearMonth("2020-05")
                .vehicleType("SEDAN")
                .vehicleInsurance(false)
                .propertyDamageLimit("THIRTY_MILLION")
                .personalInjuryAmount("THIRTY_MILLION")
                .lawyerOption(false)
                .roadService(false)
                .build();

        QuoteResponse response = quoteService.createQuote(request);

        assertNotNull(response);
        assertNotNull(response.getQuoteNo());
        assertTrue(response.getQuoteNo().startsWith("EST"));
        
        // 手計算:
        // 基本 50,000円
        // 年齢 35歳 (1.0) * 免許 GOLD (0.9) * 日常 (1.0) * 8000km (1.0) * 本人 (0.9) * 20等級 (0.8) * 事故0 (1.0) * セダン (1.0)
        // 乗算後 = 50000 * 1.0 * 0.9 * 1.0 * 1.0 * 0.9 * 0.8 * 1.0 * 1.0 = 32,400円
        // 特約加算 = なし (+0円)
        // 年間保険料 = 32,400円
        // 月額保険料 = 32,400 / 12 = 2,700円
        assertEquals(32400, response.getAnnualPremium());
        assertEquals(2700, response.getMonthlyPremium());
    }

    /**
     * UT-002: 保険料計算（高リスク条件、各種加算てんこ盛り）
     */
    @Test
    public void testUt002_HighRiskCalculation() {
        QuoteRequest request = QuoteRequest.builder()
                .driverAge(18)
                .licenseColor("GREEN")
                .usageType("BUSINESS")
                .annualMileage(15000)
                .driverRange("ANYONE")
                .hasCurrentInsurance(true)
                .grade(3)
                .accidentTerm(3)
                .maker("日産")
                .carName("エクストレイル")
                .firstRegistrationYearMonth("2024-01")
                .vehicleType("SUV")
                .vehicleInsurance(true) // +30,000
                .propertyDamageLimit("UNLIMITED") // +5,000
                .personalInjuryAmount("UNLIMITED") // +7,000
                .lawyerOption(true) // +2,000
                .roadService(true) // +1,500
                .build();

        QuoteResponse response = quoteService.createQuote(request);

        assertNotNull(response);
        
        // 手計算:
        // 基本 50,000円
        // 18歳 (1.6) * GREEN (1.1) * BUSINESS (1.25) * 15000km (1.15) * ANYONE (1.2) * 3等級 (1.3) * 事故3年 (1.2) * SUV (1.15)
        // 乗算後 = 50000 * 1.6 * 1.1 * 1.25 * 1.15 * 1.2 * 1.3 * 1.2 * 1.15 = 272,329.2円
        // 加算合計 = 30000 + 5000 + 7000 + 2000 + 1500 = 45,500円
        // 年間保険料（丸め前）= 272329.2 + 45500 = 317829.2円
        // 年額（10円未満四捨五入）= 317,830円
        // 月額（317830 / 12 = 26485.83 -> 10円未満四捨五入）= 26,490円
        assertEquals(317830, response.getAnnualPremium());
        assertEquals(26490, response.getMonthlyPremium());
    }

    /**
     * UT-003: 年齢境界値 (18/25/26/34/35/59/60)
     */
    @Test
    public void testUt003_AgeBoundaries() {
        // 年齢 25歳 (1.60)
        assertEquals(BigDecimal.valueOf(1.60).setScale(3), quoteService.getRateMasterFromDb("AGE", "18_25").getRate());
        // 年齢 26歳 (1.25)
        assertEquals(BigDecimal.valueOf(1.25).setScale(3), quoteService.getRateMasterFromDb("AGE", "26_34").getRate());
        // 年齢 35歳 (1.00)
        assertEquals(BigDecimal.valueOf(1.00).setScale(3), quoteService.getRateMasterFromDb("AGE", "35_59").getRate());
        // 年齢 60歳 (1.20)
        assertEquals(BigDecimal.valueOf(1.20).setScale(3), quoteService.getRateMasterFromDb("AGE", "60_UP").getRate());
    }

    /**
     * UT-004: 走行距離境界値 (5000/5001/10000/10001)
     */
    @Test
    public void testUt004_MileageBoundaries() {
        // 5,000km以下 (0.95)
        assertEquals(BigDecimal.valueOf(0.95).setScale(3), quoteService.getRateMasterFromDb("MILEAGE", "0_5000").getRate());
        // 5,001km〜10,000km (1.00)
        assertEquals(BigDecimal.valueOf(1.00).setScale(3), quoteService.getRateMasterFromDb("MILEAGE", "5001_10000").getRate());
        // 10,001km以上 (1.15)
        assertEquals(BigDecimal.valueOf(1.15).setScale(3), quoteService.getRateMasterFromDb("MILEAGE", "10001_UP").getRate());
    }

    /**
     * UT-005: 等級境界値 (1〜5/6〜10/11〜15/16〜20)
     */
    @Test
    public void testUt005_GradeBoundaries() {
        assertEquals(BigDecimal.valueOf(1.30).setScale(3), quoteService.getRateMasterFromDb("GRADE", "1_5").getRate());
        assertEquals(BigDecimal.valueOf(1.10).setScale(3), quoteService.getRateMasterFromDb("GRADE", "6_10").getRate());
        assertEquals(BigDecimal.valueOf(0.95).setScale(3), quoteService.getRateMasterFromDb("GRADE", "11_15").getRate());
        assertEquals(BigDecimal.valueOf(0.80).setScale(3), quoteService.getRateMasterFromDb("GRADE", "16_20").getRate());
    }

    /**
     * UT-006: 端数処理（四捨五入）の確認
     */
    @Test
    public void testUt006_RoundingLogic() {
        // 丸め処理が正しく10円未満の四捨五入で行われるか検証する
        QuoteRequest request = QuoteRequest.builder()
                .driverAge(30) // x1.25
                .licenseColor("GOLD") // x0.90
                .usageType("COMMUTE") // x1.10
                .annualMileage(4000) // x0.95
                .driverRange("SELF") // x0.90
                .hasCurrentInsurance(true)
                .grade(12) // x0.95
                .accidentTerm(0)
                .maker("スバル")
                .carName("インプレッサ")
                .firstRegistrationYearMonth("2021-10")
                .vehicleType("COMPACT") // x0.95
                .vehicleInsurance(false)
                .propertyDamageLimit("THIRTY_MILLION")
                .personalInjuryAmount("THIRTY_MILLION")
                .lawyerOption(false)
                .roadService(false)
                .build();

        // 50000 * 1.25 * 0.90 * 1.10 * 0.95 * 0.90 * 0.95 * 0.95 = 47745.07
        // 丸め前年額: 47,745.07
        // 丸め後年額 (10円未満四捨五入): 47,750円
        // 月額 (47750 / 12 = 3979.17) -> 四捨五入後: 3,980円
        QuoteResponse response = quoteService.createQuote(request);
        assertEquals(47750, response.getAnnualPremium());
        assertEquals(3980, response.getMonthlyPremium());
    }

    /**
     * UT-009: 見積番号の同日複数作成とユニーク性
     */
    @Test
    public void testUt009_UniqueQuoteNo() {
        QuoteRequest request = QuoteRequest.builder()
                .driverAge(35)
                .licenseColor("GOLD")
                .usageType("PRIVATE")
                .annualMileage(3000)
                .driverRange("SELF")
                .hasCurrentInsurance(false)
                .maker("ホンダ")
                .carName("フィット")
                .firstRegistrationYearMonth("2018-09")
                .vehicleType("COMPACT")
                .vehicleInsurance(false)
                .propertyDamageLimit("THIRTY_MILLION")
                .personalInjuryAmount("THIRTY_MILLION")
                .lawyerOption(false)
                .roadService(false)
                .build();

        QuoteResponse response1 = quoteService.createQuote(request);
        QuoteResponse response2 = quoteService.createQuote(request);

        assertNotNull(response1.getQuoteNo());
        assertNotNull(response2.getQuoteNo());
        assertNotEquals(response1.getQuoteNo(), response2.getQuoteNo());
        assertTrue(response1.getQuoteNo().matches("EST\\d{12}"));
    }

    /**
     * UT-010: 内訳情報がDBに保存されているか確認
     */
    @Test
    public void testUt010_BreakdownsSaved() {
        QuoteRequest request = QuoteRequest.builder()
                .driverAge(35)
                .licenseColor("GOLD")
                .usageType("PRIVATE")
                .annualMileage(3000)
                .driverRange("SELF")
                .hasCurrentInsurance(false)
                .maker("マツダ")
                .carName("MAZDA3")
                .firstRegistrationYearMonth("2022-04")
                .vehicleType("COMPACT")
                .vehicleInsurance(true) // 加算内訳に入るはず
                .propertyDamageLimit("THIRTY_MILLION")
                .personalInjuryAmount("THIRTY_MILLION")
                .lawyerOption(false)
                .roadService(false)
                .build();

        QuoteResponse response = quoteService.createQuote(request);

        Optional<Quote> quoteOpt = quoteRepository.findByQuoteNo(response.getQuoteNo());
        assertTrue(quoteOpt.isPresent());
        Quote quote = quoteOpt.get();

        assertFalse(quote.getBreakdowns().isEmpty());
        // 車両保険ありの加算が含まれていることを確認
        boolean hasVehicleInsuranceBreakdown = quote.getBreakdowns().stream()
                .anyMatch(b -> "VEHICLE_INSURANCE_TRUE".equals(b.getItemCode()) && b.getAmount() == 30000);
        assertTrue(hasVehicleInsuranceBreakdown);
    }
}

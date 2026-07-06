package com.ts.quotesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 見積応答DTO（一般結果と管理者向け詳細情報を統合）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponse {
    private String quoteNo;
    
    // 入力条件パラメータ
    private Integer driverAge;
    private String licenseColor;
    private String usageType;
    private Integer annualMileage;
    private String driverRange;
    private Boolean hasCurrentInsurance;
    private Integer grade;
    private Integer accidentTerm;
    private String maker;
    private String carName;
    private String firstRegistrationYearMonth;
    private String vehicleType;
    private Boolean vehicleInsurance;
    
    // 計算結果
    private Integer annualPremium;
    private Integer monthlyPremium;
    private List<BreakdownDto> breakdowns;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownDto {
        private String itemCode;
        private String itemName;
        private BigDecimal rate;
        private Integer amount;
    }
}

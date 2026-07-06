package com.ts.quotesystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 見積作成リクエストDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequest {

    @NotNull(message = "運転者年齢は必須です。")
    @Min(value = 18, message = "運転者年齢は18歳以上で指定してください。")
    @Max(value = 100, message = "運転者年齢は100歳以下で指定してください。")
    private Integer driverAge;

    @NotBlank(message = "免許証色は必須です。")
    @Pattern(regexp = "^(GOLD|BLUE|GREEN)$", message = "免許証色はGOLD、BLUE、GREENのいずれかで指定してください。")
    private String licenseColor;

    @NotBlank(message = "使用目的は必須です。")
    @Pattern(regexp = "^(PRIVATE|COMMUTE|BUSINESS)$", message = "使用目的はPRIVATE、COMMUTE、BUSINESSのいずれかで指定してください。")
    private String usageType;

    @NotNull(message = "年間走行距離は必須です。")
    @Min(value = 0, message = "年間走行距離は0km以上で指定してください。")
    @Max(value = 30000, message = "年間走行距離は30,000km以下で指定してください。")
    private Integer annualMileage;

    @NotBlank(message = "運転者範囲は必須です。")
    @Pattern(regexp = "^(SELF|COUPLE|FAMILY|ANYONE)$", message = "運転者範囲はSELF、COUPLE、FAMILY、ANYONEのいずれかで指定してください。")
    private String driverRange;

    @NotNull(message = "現在加入有無は必須です。")
    private Boolean hasCurrentInsurance;

    private Integer grade;

    private Integer accidentTerm;

    @NotBlank(message = "メーカーは必須です。")
    @Size(max = 50, message = "メーカーは50文字以内で指定してください。")
    private String maker;

    @NotBlank(message = "車名は必須です。")
    @Size(max = 50, message = "車名は50文字以内で指定してください。")
    private String carName;

    @NotBlank(message = "初度登録年月は必須です。")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "初度登録年月はYYYY-MM形式で入力してください。")
    private String firstRegistrationYearMonth;

    @NotBlank(message = "車両タイプは必須です。")
    @Pattern(regexp = "^(COMPACT|SEDAN|MINIVAN|SUV|KEI)$", message = "車両タイプはCOMPACT、SEDAN、MINIVAN、SUV、KEIのいずれかで指定してください。")
    private String vehicleType;

    @NotNull(message = "車両保険有無は必須です。")
    private Boolean vehicleInsurance;

    @NotBlank(message = "対物補償は必須です。")
    @Pattern(regexp = "^(UNLIMITED|THIRTY_MILLION)$", message = "対物補償はUNLIMITED、THIRTY_MILLIONのいずれかで指定してください。")
    private String propertyDamageLimit;

    @NotBlank(message = "人身傷害は必須です。")
    @Pattern(regexp = "^(THIRTY_MILLION|FIFTY_MILLION|UNLIMITED)$", message = "人身傷害はTHIRTY_MILLION、FIFTY_MILLION、UNLIMITEDのいずれかで指定してください。")
    private String personalInjuryAmount;

    @NotNull(message = "弁護士特約有無は必須です。")
    private Boolean lawyerOption;

    @NotNull(message = "ロードサービス有無は必須です。")
    private Boolean roadService;

    // 現在加入ありの場合、等級の相関チェック
    @AssertTrue(message = "現在加入ありの場合、等級は1〜20で指定してください。")
    @JsonIgnore
    public boolean isGradeValid() {
        if (Boolean.TRUE.equals(hasCurrentInsurance)) {
            return grade != null && grade >= 1 && grade <= 20;
        }
        return true;
    }

    // 現在加入ありの場合、事故有期間の相関チェック
    @AssertTrue(message = "現在加入ありの場合、事故有係数適用期間は0〜6で指定してください。")
    @JsonIgnore
    public boolean isAccidentTermValid() {
        if (Boolean.TRUE.equals(hasCurrentInsurance)) {
            return accidentTerm != null && accidentTerm >= 0 && accidentTerm <= 6;
        }
        return true;
    }

    // 初度登録年月の未来年月チェック
    @AssertTrue(message = "初度登録年月は現在以前の年月を指定してください。")
    @JsonIgnore
    public boolean isFirstRegistrationYearMonthValid() {
        if (firstRegistrationYearMonth == null || !firstRegistrationYearMonth.matches("^\\d{4}-\\d{2}$")) {
            return true; // 形式エラーは@Patternが処理する
        }
        try {
            LocalDate inputYm = LocalDate.parse(firstRegistrationYearMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate currentYm = LocalDate.now().withDayOfMonth(1);
            return !inputYm.isAfter(currentYm);
        } catch (Exception e) {
            return false;
        }
    }
}

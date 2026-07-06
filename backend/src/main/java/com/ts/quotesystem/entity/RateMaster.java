package com.ts.quotesystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * 保険料計算用の料率・加算額マスタ
 */
@Entity
@Table(name = "rate_masters", indexes = {
    @Index(name = "idx_rate_masters_lookup", columnList = "category, item_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;   // 例: AGE, LICENSE, USAGE, MILEAGE, RANGE, GRADE, ACCIDENT, VEHICLE_TYPE, COVERAGE

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;   // 例: 18_25, GOLD, PRIVATE, etc.

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;   // 例: 18〜25歳, ゴールド, etc.

    @Column(precision = 6, scale = 3)
    private BigDecimal rate;   // 補正係数 (乗算用、例: 1.600)

    private Integer amount;    // 加算額 (加算用、例: 30000)

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}

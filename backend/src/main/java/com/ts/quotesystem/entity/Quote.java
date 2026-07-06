package com.ts.quotesystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 見積ヘッダ情報
 */
@Entity
@Table(name = "quotes", indexes = {
    @Index(name = "idx_quotes_no", columnList = "quote_no"),
    @Index(name = "idx_quotes_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "breakdowns")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_no", unique = true, nullable = false, length = 20)
    private String quoteNo;

    @Column(name = "driver_age", nullable = false)
    private Integer driverAge;

    @Column(name = "license_color", nullable = false, length = 20)
    private String licenseColor;

    @Column(name = "usage_type", nullable = false, length = 20)
    private String usageType;

    @Column(name = "annual_mileage", nullable = false)
    private Integer annualMileage;

    @Column(name = "driver_range", nullable = false, length = 20)
    private String driverRange;

    @Column(name = "has_current_insurance", nullable = false)
    private Boolean hasCurrentInsurance;

    private Integer grade;

    @Column(name = "accident_term")
    private Integer accidentTerm;

    @Column(nullable = false, length = 50)
    private String maker;

    @Column(name = "car_name", nullable = false, length = 50)
    private String carName;

    @Column(name = "first_registration_ym", nullable = false, length = 7)
    private String firstRegistrationYm;

    @Column(name = "vehicle_type", nullable = false, length = 20)
    private String vehicleType;

    @Column(name = "vehicle_insurance", nullable = false)
    private Boolean vehicleInsurance;

    @Column(name = "annual_premium", nullable = false)
    private Integer annualPremium;

    @Column(name = "monthly_premium", nullable = false)
    private Integer monthlyPremium;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuoteBreakdown> breakdowns = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 双方向関連管理用メソッド
    public void addBreakdown(QuoteBreakdown breakdown) {
        breakdowns.add(breakdown);
        breakdown.setQuote(this);
    }
}

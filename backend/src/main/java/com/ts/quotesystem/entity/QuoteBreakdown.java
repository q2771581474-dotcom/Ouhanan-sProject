package com.ts.quotesystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * 見積の計算内訳（適用した料率・加算額の詳細）
 */
@Entity
@Table(name = "quote_breakdowns", indexes = {
    @Index(name = "idx_quote_breakdowns_quote", columnList = "quote_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(precision = 6, scale = 3)
    private BigDecimal rate;   // 適用した係数

    private Integer amount;    // 適用した加算額

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}

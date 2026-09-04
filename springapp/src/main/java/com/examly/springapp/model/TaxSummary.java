package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tax_summaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TaxSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "phone"})
    private User user;

    private int financialYear;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal ltcg = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal stcg = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal interestIncome = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal section80c = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal section80d = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal hraExemption = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal taxableIncome = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedTax = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal advanceTaxPaid = BigDecimal.ZERO;
}

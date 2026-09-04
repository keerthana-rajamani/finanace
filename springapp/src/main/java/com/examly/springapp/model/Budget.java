package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "phone"})
    private User user;

    @NotBlank
    private String category;

    @Column(name = "\"month\"", nullable = false)
    private LocalDate month;

    @NotNull
    @DecimalMin(value = "0.01", message = "Budget amount must be a positive number")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal budgetAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    private int alertAtPercent = 80;

    private boolean carryForward = false;
}

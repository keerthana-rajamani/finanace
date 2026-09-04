package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "phone"})
    private User user;

    @NotBlank
    private String name;

    private String category;

    @NotNull
    @DecimalMin(value = "0.01", message = "Bill amount must be a positive number")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Min(value = 1) @Max(value = 31)
    private Integer dueDayOfMonth;

    @Enumerated(EnumType.STRING)
    private Recurrence recurrence = Recurrence.MONTHLY;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private LocalDate lastPaidDate;

    private LocalDate nextDueDate;

    private boolean reminderSent = false;

    public enum Recurrence { MONTHLY, QUARTERLY, ANNUAL }
    public enum Status { PENDING, PAID, OVERDUE }
}

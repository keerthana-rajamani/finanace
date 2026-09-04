package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "phone"})
    private User user;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    @DecimalMin(value = "0.01", message = "Target amount must exceed current savings")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal targetAmount;

    @NotNull
    private LocalDate targetDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    public enum Priority { HIGH, MEDIUM, LOW }
    public enum Status { ACTIVE, COMPLETED, PAUSED }
}

package com.examly.springapp.repository;

import com.examly.springapp.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByUserId(Long userId);
    List<Bill> findByUserIdAndNextDueDateBetween(Long userId, LocalDate from, LocalDate to);
    List<Bill> findByUserIdAndStatus(Long userId, Bill.Status status);
    Optional<Bill> findByIdAndUserId(Long id, Long userId);
}

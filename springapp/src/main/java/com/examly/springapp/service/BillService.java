package com.examly.springapp.service;

import com.examly.springapp.exception.*;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BillService {

    private final BillRepository billRepository;
    private final UserRepository userRepository;

    public BillService(BillRepository billRepository, UserRepository userRepository) {
        this.billRepository = billRepository;
        this.userRepository = userRepository;
    }

    public Bill createBill(Long userId, Bill bill) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        bill.setUser(user);
        bill.setNextDueDate(calculateNextDueDate(bill.getDueDayOfMonth()));
        bill.setStatus(Bill.Status.PENDING);
        return billRepository.save(bill);
    }

    public List<Bill> getBills(Long userId) {
        return billRepository.findByUserId(userId);
    }

    public List<Bill> getUpcomingBills(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);
        return billRepository.findByUserIdAndNextDueDateBetween(userId, today, sevenDaysLater);
    }

    public Bill markAsPaid(Long billId, Long userId) {
        Bill bill = billRepository.findByIdAndUserId(billId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        bill.setStatus(Bill.Status.PAID);
        bill.setLastPaidDate(LocalDate.now());
        bill.setNextDueDate(calculateNextDueDate(bill.getDueDayOfMonth(), bill.getRecurrence()));
        return billRepository.save(bill);
    }

    public Bill updateBill(Long billId, Long userId, Bill updated) {
        Bill bill = billRepository.findByIdAndUserId(billId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        bill.setName(updated.getName());
        bill.setCategory(updated.getCategory());
        bill.setAmount(updated.getAmount());
        bill.setDueDayOfMonth(updated.getDueDayOfMonth());
        bill.setRecurrence(updated.getRecurrence());
        bill.setNextDueDate(calculateNextDueDate(updated.getDueDayOfMonth()));
        bill.setStatus(Bill.Status.PENDING);
        return billRepository.save(bill);
    }

    public void deleteBill(Long billId, Long userId) {
        Bill bill = billRepository.findByIdAndUserId(billId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        billRepository.delete(bill);
    }

    private LocalDate calculateNextDueDate(int dueDayOfMonth) {
        LocalDate today = LocalDate.now();
        LocalDate dueThisMonth = today.withDayOfMonth(Math.min(dueDayOfMonth, today.lengthOfMonth()));
        return dueThisMonth.isBefore(today) ? dueThisMonth.plusMonths(1) : dueThisMonth;
    }

    private LocalDate calculateNextDueDate(int dueDayOfMonth, Bill.Recurrence recurrence) {
        LocalDate today = LocalDate.now();
        int months = recurrence == Bill.Recurrence.QUARTERLY ? 3 : recurrence == Bill.Recurrence.ANNUAL ? 12 : 1;
        return today.plusMonths(months).withDayOfMonth(Math.min(dueDayOfMonth, today.plusMonths(months).lengthOfMonth()));
    }
}

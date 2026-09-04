package com.examly.springapp.controller;

import com.examly.springapp.model.Bill;
import com.examly.springapp.service.BillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;
    private final AuthHelper authHelper;

    public BillController(BillService billService, AuthHelper authHelper) {
        this.billService = billService;
        this.authHelper = authHelper;
    }

    @PostMapping
    public ResponseEntity<Bill> createBill(@RequestBody Bill bill) {
        return ResponseEntity.ok(billService.createBill(authHelper.getCurrentUserId(), bill));
    }

    @GetMapping
    public ResponseEntity<List<Bill>> getBills() {
        return ResponseEntity.ok(billService.getBills(authHelper.getCurrentUserId()));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Bill>> getUpcoming() {
        return ResponseEntity.ok(billService.getUpcomingBills(authHelper.getCurrentUserId()));
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<Bill> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(billService.markAsPaid(id, authHelper.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bill> updateBill(@PathVariable Long id, @RequestBody Bill bill) {
        return ResponseEntity.ok(billService.updateBill(id, authHelper.getCurrentUserId(), bill));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBill(@PathVariable Long id) {
        billService.deleteBill(id, authHelper.getCurrentUserId());
        return ResponseEntity.ok("Bill deleted");
    }
}

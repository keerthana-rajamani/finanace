package com.examly.springapp.controller;

import com.examly.springapp.model.TaxSummary;
import com.examly.springapp.service.TaxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tax")
public class TaxController {

    private final TaxService taxService;
    private final AuthHelper authHelper;

    public TaxController(TaxService taxService, AuthHelper authHelper) {
        this.taxService = taxService;
        this.authHelper = authHelper;
    }

    @GetMapping("/summary")
    public ResponseEntity<TaxSummary> getSummary(@RequestParam(required = false) Integer year) {
        int fy = year != null ? year : (LocalDate.now().getMonthValue() >= 4 ? LocalDate.now().getYear() : LocalDate.now().getYear() - 1);
        return ResponseEntity.ok(taxService.getOrComputeTaxSummary(authHelper.getCurrentUserId(), fy));
    }

    @GetMapping("/all")
    public ResponseEntity<List<TaxSummary>> getAll() {
        return ResponseEntity.ok(taxService.getAllTaxSummaries(authHelper.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<TaxSummary> saveSummary(@RequestBody TaxSummary taxSummary) {
        return ResponseEntity.ok(taxService.saveTaxSummary(authHelper.getCurrentUserId(), taxSummary));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaxSummary> updateSummary(@PathVariable Long id, @RequestBody TaxSummary taxSummary) {
        return ResponseEntity.ok(taxService.updateTaxSummary(id, authHelper.getCurrentUserId(), taxSummary));
    }
}

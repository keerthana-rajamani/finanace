package com.examly.springapp.controller;

import com.examly.springapp.model.Account;
import com.examly.springapp.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AuthHelper authHelper;

    public AccountController(AccountService accountService, AuthHelper authHelper) {
        this.accountService = accountService;
        this.authHelper = authHelper;
    }

    @PostMapping({"", "/link"})
    public ResponseEntity<Account> addAccount(@RequestBody Account account) {
        return ResponseEntity.ok(accountService.addAccount(authHelper.getCurrentUserId(), account));
    }

    @GetMapping
    public ResponseEntity<List<Account>> getAccounts() {
        return ResponseEntity.ok(accountService.getAccounts(authHelper.getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id, authHelper.getCurrentUserId());
        return ResponseEntity.ok("Account unlinked successfully");
    }
}

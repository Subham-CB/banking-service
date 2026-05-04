package com.banking.nationalBank.controller;

import com.banking.nationalBank.dto.*;
import com.banking.nationalBank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Banking account operations")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create a new bank account")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account details")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @PostMapping("/{accountNumber}/credit")
    @Operation(summary = "Credit (deposit) money into an account")
    public ResponseEntity<AccountResponse> credit(
            @PathVariable String accountNumber,
            @Valid @RequestBody CreditDebitRequest request) {
        return ResponseEntity.ok(accountService.credit(accountNumber, request));
    }

    @PostMapping("/{accountNumber}/debit")
    @Operation(summary = "Debit (withdraw) money from an account")
    public ResponseEntity<AccountResponse> debit(
            @PathVariable String accountNumber,
            @Valid @RequestBody CreditDebitRequest request) {
        return ResponseEntity.ok(accountService.debit(accountNumber, request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money between two accounts")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        accountService.transfer(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{accountNumber}/transactions")
    @Operation(summary = "Get transaction history for an account")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getTransactions(accountNumber));
    }
}

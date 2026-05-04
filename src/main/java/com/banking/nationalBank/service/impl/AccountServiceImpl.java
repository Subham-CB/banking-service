package com.banking.nationalBank.service.impl;

import com.banking.nationalBank.dto.*;
import com.banking.nationalBank.entity.Account;
import com.banking.nationalBank.entity.Transaction;
import com.banking.nationalBank.entity.enums.TransactionType;
import com.banking.nationalBank.exception.AccountNotFoundException;
import com.banking.nationalBank.exception.InsufficientFundsException;
import com.banking.nationalBank.repository.AccountRepository;
import com.banking.nationalBank.repository.TransactionRepository;
import com.banking.nationalBank.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final int ACCOUNT_NUMBER_DIGITS = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long ACCOUNT_NUMBER_BOUND = (long) Math.pow(10, ACCOUNT_NUMBER_DIGITS);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String accountNumber = generateAccountNumber();
        BigDecimal initialBalance = request.getInitialDeposit() != null
                ? request.getInitialDeposit()
                : BigDecimal.ZERO;

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountHolderName(request.getAccountHolderName().trim())
                .balance(initialBalance)
                .build();

        account = accountRepository.save(account);

        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            recordTransaction(account.getAccountNumber(), TransactionType.CREDIT,
                    initialBalance, initialBalance, "Initial deposit");
        }

        return mapToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        Account account = findAccount(accountNumber);
        return mapToResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse credit(String accountNumber, CreditDebitRequest request) {
        Account account = findAccount(accountNumber);
        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        account = accountRepository.save(account);
        recordTransaction(accountNumber, TransactionType.CREDIT,
                request.getAmount(), newBalance, request.getDescription());
        return mapToResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse debit(String accountNumber, CreditDebitRequest request) {
        Account account = findAccount(accountNumber);
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(accountNumber, account.getBalance(), request.getAmount());
        }
        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        account = accountRepository.save(account);
        recordTransaction(accountNumber, TransactionType.DEBIT,
                request.getAmount(), newBalance, request.getDescription());
        return mapToResponse(account);
    }

    @Override
    @Transactional
    public void transfer(TransferRequest request) {
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        Account source = findAccount(request.getFromAccountNumber());
        Account destination = findAccount(request.getToAccountNumber());

        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    request.getFromAccountNumber(), source.getBalance(), request.getAmount());
        }

        BigDecimal sourceNewBalance = source.getBalance().subtract(request.getAmount());
        BigDecimal destNewBalance = destination.getBalance().add(request.getAmount());

        source.setBalance(sourceNewBalance);
        destination.setBalance(destNewBalance);
        accountRepository.save(source);
        accountRepository.save(destination);

        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Transfer";

        recordTransaction(source.getAccountNumber(), TransactionType.TRANSFER_DEBIT,
                request.getAmount(), sourceNewBalance,
                description + " to " + destination.getAccountNumber());
        recordTransaction(destination.getAccountNumber(), TransactionType.TRANSFER_CREDIT,
                request.getAmount(), destNewBalance,
                description + " from " + source.getAccountNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(String accountNumber) {
        findAccount(accountNumber);
        return transactionRepository
                .findByAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    private Account findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private void recordTransaction(String accountNumber, TransactionType type,
                                   BigDecimal amount, BigDecimal balanceAfter, String description) {
        transactionRepository.save(Transaction.builder()
                .accountNumber(accountNumber)
                .transactionType(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .build());
    }

    private String generateAccountNumber() {
        String candidate;
        do {
            // Generate a random ACCOUNT_NUMBER_DIGITS-digit number, zero-padded
            long number = (long) (SECURE_RANDOM.nextDouble() * ACCOUNT_NUMBER_BOUND);
            candidate = String.format("%0" + ACCOUNT_NUMBER_DIGITS + "d", number);
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .accountNumber(transaction.getAccountNumber())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}

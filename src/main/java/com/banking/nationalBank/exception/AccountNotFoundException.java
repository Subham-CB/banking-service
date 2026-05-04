package com.banking.nationalBank.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountNumber) {
        super("Account not found with account number: " + accountNumber);
    }
}

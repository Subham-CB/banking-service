package com.banking.nationalBank.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String accountNumber, BigDecimal balance, BigDecimal requested) {
        super(String.format(
                "Insufficient funds in account %s. Available balance: %.2f, Requested: %.2f",
                accountNumber, balance, requested));
    }
}

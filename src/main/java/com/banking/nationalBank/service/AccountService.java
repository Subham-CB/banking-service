package com.banking.nationalBank.service;

import com.banking.nationalBank.dto.*;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(String accountNumber);

    AccountResponse credit(String accountNumber, CreditDebitRequest request);

    AccountResponse debit(String accountNumber, CreditDebitRequest request);

    void transfer(TransferRequest request);

    List<TransactionResponse> getTransactions(String accountNumber);
}
